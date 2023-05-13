package cn.edu.scut.agent;


import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import cn.edu.scut.util.FileUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@Component
@Lazy
@Slf4j
public class MultiAgentBuffer implements InitializingBean {


    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    private int observationShape;
    private int offloadingShape;
    private int actionShape;

    @Value("${rl.buffer-size}")
    private int bufferSize;

    @Value("${rl.batch-size}")
    private int batchSize;
    @Value("${env.max-task-redundancy}")
    private int maxTaskRedundancy;
    @Value("${hadoop.hdfs.url}")
    private String hdfsUrl;

    @Getter
    private float[][][] observations;
    @Getter
    private int[][][] actions;
    @Getter
    private float[][][] rewards;
    @Getter
    private int[][][] availActions;
    @Getter
    private float[][][] nextObservations;

    private int index = 0;

    private int size = 0;

    @Autowired
    private FileSystem fileSystem;

    @Autowired
    private Random bufferRandom;

    @Value("${rl.observation.add-reliability-requirement}")
    private boolean addReliabilityRequirement;

    @Value("${env.use-task-reliability}")
    private boolean useTaskReliability;


    @Override
    public void afterPropertiesSet() {
        offloadingShape = edgeNodeNumber + 1;
        actionShape = offloadingShape * maxTaskRedundancy;
        observationShape = edgeNodeNumber * 6 + 2;
        if (addReliabilityRequirement) {
            observationShape += 1;
        }
        if (useTaskReliability) {
            observationShape += 1;
        }
        observations = new float[bufferSize][edgeNodeNumber][observationShape];
        actions = new int[bufferSize][edgeNodeNumber][maxTaskRedundancy];
        availActions = new int[bufferSize][edgeNodeNumber][actionShape];
        rewards = new float[bufferSize][edgeNodeNumber][1];
        nextObservations = new float[bufferSize][edgeNodeNumber][observationShape];
    }

    public void insert(float[][] observation, int[][] action, int[][] availAction, float[][] reward, float[][] nextObservation) {
        observations[index] = observation;
        actions[index] = action;
        availActions[index] = availAction;
        rewards[index] = reward;
        nextObservations[index] = nextObservation;
        index = (index + 1) % bufferSize;
        size = Math.min(size + 1, bufferSize);
    }

    // off-policy
    public NDList sample(NDManager manager) {
        var list = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        Collections.shuffle(list, bufferRandom);
        var batch = new int[batchSize];
        for (int i = 0; i < batchSize; i++) {
            batch[i] = list.get(i);
        }

        var ndStates = manager.zeros(new Shape(batchSize, edgeNodeNumber, observationShape), DataType.FLOAT32);
        var ndActions = manager.zeros(new Shape(batchSize, edgeNodeNumber, maxTaskRedundancy), DataType.INT32);
        var ndAvailActions = manager.zeros(new Shape(batchSize, edgeNodeNumber, actionShape), DataType.INT32);
        var ndRewards = manager.zeros(new Shape(batchSize, edgeNodeNumber, 1), DataType.FLOAT32);
        var ndNextStates = manager.zeros(new Shape(batchSize, edgeNodeNumber, observationShape), DataType.FLOAT32);

        for (int i = 0; i < batchSize; i++) {
            var ndIndex = new NDIndex(i);
            ndStates.set(ndIndex, manager.create(observations[batch[i]]));
            ndActions.set(ndIndex, manager.create(actions[batch[i]]));
            ndAvailActions.set(ndIndex, manager.create(availActions[batch[i]]));
            ndRewards.set(ndIndex, manager.create(rewards[batch[i]]));
            ndNextStates.set(ndIndex, manager.create(nextObservations[batch[i]]));
        }
        return new NDList(ndStates, ndActions, ndAvailActions, ndRewards, ndNextStates);
    }

    // on-policy
    public NDList sampleAll(NDManager manager) {
        var ndObservations = manager.zeros(new Shape(bufferSize, edgeNodeNumber, observationShape), DataType.FLOAT32);
        var ndActions = manager.zeros(new Shape(bufferSize, edgeNodeNumber, maxTaskRedundancy), DataType.INT32);
        var ndAvailActions = manager.zeros(new Shape(bufferSize, edgeNodeNumber, actionShape), DataType.INT32);
        var ndRewards = manager.zeros(new Shape(bufferSize, edgeNodeNumber, 1), DataType.FLOAT32);
        var ndNextObservations = manager.zeros(new Shape(bufferSize, edgeNodeNumber, observationShape), DataType.FLOAT32);
        for (int i = 0; i < bufferSize; i++) {
            var ndIndex = new NDIndex(i);
            ndObservations.set(ndIndex, manager.create(observations[i]));
            ndActions.set(ndIndex, manager.create(actions[i]));
            ndAvailActions.set(ndIndex, manager.create(availActions[i]));
            ndRewards.set(ndIndex, manager.create(rewards[i]));
            ndNextObservations.set(ndIndex, manager.create(nextObservations[i]));
        }
        return new NDList(ndObservations, ndActions, ndAvailActions, ndRewards, ndNextObservations);
    }

    public void saveHdfs(String flag) {
        var path = Paths.get("results", "buffer", flag);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String basePath = "results/buffer/" + flag + "/";
        try {
            FileUtils.writeObject(observations, Paths.get("results", "buffer", flag, "states.array"));
            fileSystem.copyFromLocalFile(true, true, new Path(basePath + "states.array"), new Path(hdfsUrl + "/" + basePath + "states.array"));

            FileUtils.writeObject(actions, Paths.get("results", "buffer", flag, "actions.array"));
            fileSystem.copyFromLocalFile(true, true, new Path(basePath + "actions.array"), new Path(hdfsUrl + "/" + basePath + "actions.array"));

            FileUtils.writeObject(availActions, Paths.get("results", "buffer", flag, "availActions.array"));
            fileSystem.copyFromLocalFile(true, true, new Path(basePath + "availActions.array"), new Path(hdfsUrl + "/" + basePath + "availActions.array"));

            FileUtils.writeObject(rewards, Paths.get("results", "buffer", flag, "rewards.array"));
            fileSystem.copyFromLocalFile(true, true, new Path(basePath + "rewards.array"), new Path(hdfsUrl + "/" + basePath + "rewards.array"));

            FileUtils.writeObject(nextObservations, Paths.get("results", "buffer", flag, "nextStates.array"));
            fileSystem.copyFromLocalFile(true, true, new Path(basePath + "nextStates.array"), new Path(hdfsUrl + "/" + basePath + "nextStates.array"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadHdfs(String bufferPath) {
        var path = Paths.get("results", "buffer", bufferPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String basePath = "results/buffer/" + bufferPath + "/";
        try {
            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + basePath + "states.array"), new Path(basePath + "states.array"), false);
            observations = (float[][][]) FileUtils.readObject(Paths.get("results", "buffer", bufferPath, "states.array"));

            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + basePath + "actions.array"), new Path(basePath + "actions.array"), false);
            actions = (int[][][]) FileUtils.readObject(Paths.get("results", "buffer", bufferPath, "actions.array"));

            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + basePath + "availActions.array"), new Path(basePath + "availActions.array"), false);
            availActions = (int[][][]) FileUtils.readObject(Paths.get("results", "buffer", bufferPath, "availActions.array"));

            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + basePath + "rewards.array"), new Path(basePath + "rewards.array"), false);
            rewards = (float[][][]) FileUtils.readObject(Paths.get("results", "buffer", bufferPath, "rewards.array"));

            fileSystem.copyToLocalFile(false, new Path(hdfsUrl + "/" + basePath + "nextStates.array"), new Path(basePath + "nextStates.array"), false);
            nextObservations = (float[][][]) FileUtils.readObject(Paths.get("results", "buffer", bufferPath, "nextStates.array"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        index = bufferSize;
        size = bufferSize;
    }
}
