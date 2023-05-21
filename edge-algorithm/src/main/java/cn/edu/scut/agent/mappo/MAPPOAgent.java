package cn.edu.scut.agent.mappo;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.NoopTranslator;
import ai.djl.translate.TranslateException;
import cn.edu.scut.agent.MultiAgentAdaptor;
import cn.edu.scut.agent.MultiAgentBuffer;
import cn.edu.scut.service.TaskService;
import cn.edu.scut.util.DJLUtils;
import cn.edu.scut.util.EnvUtils;
import cn.edu.scut.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@ConditionalOnProperty(name = "rl.name", havingValue = "mappo")
public class MAPPOAgent extends MultiAgentAdaptor implements InitializingBean {

    @Autowired
    private Random schedulerRandom;

    @Autowired
    private NDManager manager;

    @Autowired
    private MultiAgentBuffer buffer;

    @Value("${rl.use-normalized-reward}")
    private boolean useNormalizedReward;

    @Value("${rl.epoch}")
    private int epoch;

    @Value("${rl.clip}")
    private float clip;

    @Value("${rl.use-entropy}")
    private boolean useEntropy;

    @Value("${rl.entropy-coef}")
    private float entropyCoef;

    @Value("${rl.gamma}")
    private float gamma;

    @Value("${env.time-slot-number}")
    private int timeSlotNumber;

    @Value("${rl.use-gae}")
    private boolean useGae;

    @Value("${rl.gae-lambda}")
    private float gaeLambda;

    @Value("${hadoop.hdfs.url}")
    private String hdfsUrl;

    @Value("${spring.application.name}")
    String name;

    @Value("${env.use-redundancy}")
    private boolean useRedundancy;

    @Value("${env.max-task-redundancy}")
    private int maxTaskRedundancy;

    @Value("${rl.batch-size}")
    private int batchSize;

    @Autowired
    private Model actorModel;

    @Autowired
    private Model criticModel;

    @Autowired
    private FileSystem fileSystem;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TrainingConfig trainingConfig;

    private final Lock lock = new ReentrantLock();

    private int offloadingShape;

    @Autowired
    private EnvUtils envUtils;


    // env
    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    @Override
    public void afterPropertiesSet() {
        offloadingShape = edgeNodeNumber + 1;
    }

    @Override
    public int[] selectAction(float[] state, int[] availAction, boolean training, int taskId) {
        if (useRedundancy) {
            return selectMultiAction(state, availAction, training, taskId);
        } else {
            return selectSingleAction(state, availAction, training, taskId);
        }
    }

    private int[] selectSingleAction(float[] state, int[] availAction, boolean training, int taskId) {
        int action;
        try {
            lock.lock();
            var subManager = manager.newSubManager();
            try (subManager) {
                var predictor = actorModel.newPredictor(new NoopTranslator());
                try (predictor) {
                    NDArray out = null;
                    try {
                        out = predictor.predict(new NDList(subManager.create(state))).singletonOrThrow();
                    } catch (TranslateException e) {
                        log.error("predict error: {}", e.getMessage());
                    }
                    var bool = subManager.create(availAction).eq(0);
                    assert out != null;
                    out.set(bool, -1e8f);
                    var prob = out.softmax(-1);
                    action = DJLUtils.sampleMultinomial(schedulerRandom, prob);
                }
            }
        } finally {
            lock.unlock();
        }
        return new int[]{action + 1};
    }

    private int[] selectMultiAction(float[] state, int[] availAction, boolean training, int taskId) {
        var actions = new int[maxTaskRedundancy];
        // edge node 11 , 虚拟节点
        Arrays.fill(actions, offloadingShape);
        var task = taskService.getById(taskId);
        var set = new HashSet<Integer>();
        var manager = NDManager.newBaseManager();
        try (manager) {
            var predictor = actorModel.newPredictor(new NoopTranslator());
            try (predictor) {
                NDArray out;
                try {
                    out = predictor.predict(new NDList(manager.create(state))).singletonOrThrow();
                } catch (TranslateException e) {
                    throw new RuntimeException(e);
                }
                var multiOut = out.reshape(new Shape(maxTaskRedundancy, offloadingShape));
                for (int i = 0; i < maxTaskRedundancy; i++) {
                    var bool = manager.create(availAction).eq(0).reshape(new Shape(maxTaskRedundancy, offloadingShape));
                    multiOut.set(bool, -1e8f);
                    var prob = multiOut.softmax(-1);
                    log.info("prob {}", prob);
                    // edge node 从 1 开始计数
                    var edgeNodeId = DJLUtils.sampleMultinomial(schedulerRandom, prob.get(i)) + 1;
                    set.add(edgeNodeId);
                    actions[i] = edgeNodeId;
                    // index 从0开始计数
                    envUtils.maskEdgeNode(edgeNodeId - 1, availAction);
                    if (envUtils.isMeetReliabilityRequirement(set, task)) {
                        break;
                    }
                }
            }
        }
        return actions;
    }

    public void train() {
        try {
            lock.lock();
            var subManager = manager.newSubManager();
            try (subManager) {
                var list = buffer.sampleAll(subManager);
                var states = list.get(0);
                var actions = list.get(1);
                if (useRedundancy) {
                    actions = actions.expandDims(-1);
                }
                var availActions = list.get(2);
                var rewards = list.get(3);
                var nextStates = list.get(4);
                // Trick: normalized reward
                if (useNormalizedReward) {
                    var mean = rewards.mean();
                    var std = rewards.sub(mean).pow(2).mean().sqrt().add(1e-5f);
                    rewards = rewards.sub(mean).div(std);
                }
                var criticTrainer = criticModel.newTrainer(trainingConfig);
                var actorTrainer = actorModel.newTrainer(trainingConfig);
                try (criticTrainer; actorTrainer) {
                    var stateValues = criticTrainer.evaluate(new NDList(states)).singletonOrThrow();
                    var nextStateValues = criticTrainer.evaluate(new NDList(nextStates)).singletonOrThrow();
                    // gae
                    NDArray advantages;
                    if (useGae) {
                        advantages = DJLUtils.getGae(rewards, stateValues, nextStateValues, subManager, gamma, timeSlotNumber, gaeLambda);
                    } else {
                        var returns = DJLUtils.getReturn(rewards, nextStateValues, gamma, timeSlotNumber);
                        advantages = returns.sub(stateValues);
                    }
                    var targets = advantages.add(stateValues);
                    var out = actorTrainer.evaluate(new NDList(states)).singletonOrThrow();
                    var index1 = availActions.eq(0);
                    out.set(index1, -1e5f);
                    if (useRedundancy) {
                        out = out.reshape(new Shape(batchSize, edgeNodeNumber, maxTaskRedundancy, offloadingShape));
                    }
                    var logProb = out.logSoftmax(-1);
                    var oldLogProbTaken = logProb.gather(actions, -1);
                    if (useRedundancy) {
                        oldLogProbTaken = oldLogProbTaken.sum(new int[]{-1});
                    }
                    for (int i = 0; i < epoch; i++) {
                        var gradientCollector = actorTrainer.newGradientCollector();
                        try (gradientCollector) {
                            var output = actorTrainer.forward(new NDList(states)).singletonOrThrow();
                            var index = availActions.eq(0);
                            output.set(index, -1e5f);
                            if (useRedundancy) {
                                output = output.reshape(new Shape(batchSize, edgeNodeNumber, maxTaskRedundancy, offloadingShape));
                            }
                            var probabilities = output.softmax(-1);
                            // attention！！！
                            var logProbabilities = output.logSoftmax(-1);
                            var logProbTaken = logProbabilities.gather(actions, -1);
                            if (useRedundancy) {
                                logProbTaken = logProbTaken.sum(new int[]{-1});
                            }
                            // PPO
                            var ratios = logProbTaken.sub(oldLogProbTaken).exp();
                            //
                            var surr1 = ratios.mul(advantages);
                            var surr2 = ratios.clip(1 - clip, 1 + clip).mul(advantages);
                            // maximizing
                            var loss = NDArrays.minimum(surr1, surr2).mean().neg();
                            // Trick: entropy
                            if (useEntropy) {
                                // entropy definition:
                                var entropy = probabilities.mul(logProbabilities).neg();
                                if (useRedundancy) {
                                    entropy = entropy.sum(new int[]{-1});
                                }
                                // maximizing entropy
                                var entropyLoss = entropy.mean().mul(entropyCoef).neg();
                                loss = loss.add(entropyLoss);
                            }
                            gradientCollector.backward(loss);
                            actorTrainer.step();
                        }
                        var criticGradientCollector = criticTrainer.newGradientCollector();
                        try (criticGradientCollector) {
                            var newValues = criticTrainer.forward(new NDList(states)).singletonOrThrow();
                            var criticLoss = newValues.sub(targets).pow(2).mean().mul(0.5f);
                            criticGradientCollector.backward(criticLoss);
                            criticTrainer.step();
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void saveModel(String flag) {
        String basePath = "results/model/" + flag;
        try {
            actorModel.save(Paths.get(basePath), null);
            criticModel.save(Paths.get(basePath), null);
        } catch (IOException e) {
            log.info("save model: {}", e.getMessage());
        }
    }

    public void loadModel(String flag) {
        String basePath = "results/model/" + flag;
        try {
            actorModel.load(Paths.get(basePath));
            criticModel.load(Paths.get(basePath));
        } catch (IOException | MalformedModelException e) {
            log.info("load model: {}", e.getMessage());
        }
    }

    public void saveHdfsModel(String flag) {
        String basePath = "results/model/" + flag;
        try {
            actorModel.save(Paths.get(basePath), null);
            criticModel.save(Paths.get(basePath), null);
        } catch (IOException e) {
            log.error("save model error: {}", e.getMessage());
        }
        var actorPath = basePath + "/actor-0000.params";
        var criticPath = basePath + "/critic-0000.params";
        try {
            if (fileSystem.exists(new Path(hdfsUrl + "/" + basePath))) {
                fileSystem.delete(new Path(hdfsUrl + "/" + basePath), true);
            }
        } catch (IOException e) {
            log.error("delete file in file system error: {}", e.getMessage());
        }
        try {
            fileSystem.copyFromLocalFile(true, true, new Path(actorPath), new Path(hdfsUrl + "/" + actorPath));
            fileSystem.copyFromLocalFile(true, true, new Path(criticPath), new Path(hdfsUrl + "/" + criticPath));
        } catch (IOException e) {
            log.error("file system save file error: {}", e.getMessage());
        }
    }

    public void loadHdfsModel(String flag) {
        String basePath = "results/model/" + flag;
        try {
            var actorPath = basePath + "/actor-0000.params";
            var criticPath = basePath + "/critic-0000.params";
            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + actorPath), new Path(actorPath), true);
            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + criticPath), new Path(criticPath), true);
        } catch (IOException e) {
            log.info("load hdfs model: {}", e.getMessage());
        }
        try {
            actorModel.load(Paths.get(basePath));
            criticModel.load(Paths.get(basePath));
        } catch (IOException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
        var parameters = actorModel.getBlock().getParameters();
        for (String key : parameters.keys()) {
            parameters.get(key).getArray().setRequiresGradient(true);
        }
        var parameters2 = criticModel.getBlock().getParameters();
        for (String key : parameters2.keys()) {
            parameters.get(key).getArray().setRequiresGradient(true);
        }
        FileUtils.recursiveDelete(Paths.get(basePath).toFile());
    }

    public void loadSteamModel(InputStream inputStream, String fileName) {
        switch (fileName) {
            case "actor.param" -> {
                try {
                    actorModel.load(inputStream);
                } catch (IOException | MalformedModelException e) {
                    log.info("load stream model error: {}", e.getMessage());
                }
            }
            case "critic.param" -> {
                try {
                    criticModel.load(inputStream);
                } catch (IOException | MalformedModelException e) {
                    log.info("load stream model error: {}", e.getMessage());
                }
            }
            default -> throw new RuntimeException("fileName error!");
        }
    }
}
