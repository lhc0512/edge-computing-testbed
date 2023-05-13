package cn.edu.scut.service;


import cn.edu.scut.agent.MultiAgentBuffer;
import cn.edu.scut.bean.Task;
import cn.edu.scut.util.MathUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;

@Service
@Slf4j
public class RunnerService implements InitializingBean {
    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;
    @Value("${env.time-slot-number}")
    private int timeSlotNumber;
    @Value("${env.time-slot}")
    private int timeSlotLen;
    @Value("${env.test-number}")
    private int testNumber;
    // heuristic-test
    @Value("${rl.state-shape:0}")
    private int stateShape;
    //
    private int actionShape;
    private int offloadingShape;
    private int observationShape;

    @Value("${env.max-task-redundancy}")
    private int maxTaskRedundancy;

    @Value("${rl.use-reward-in-time:false}")
    private boolean useRewardInTime;

    @Value("${env.use-redundancy}")
    private boolean useRedundancy;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private TaskService taskService;
    @Lazy
    @Autowired(required = false)
    private MultiAgentBuffer buffer;
    @Lazy
    @Autowired
    private TransitionService transitionService;

    @Override
    public void afterPropertiesSet() {
        observationShape = 6 * edgeNodeNumber + 3;
        offloadingShape = edgeNodeNumber + 1;
        actionShape = offloadingShape * maxTaskRedundancy;
    }

    public void init() {
        var controllerUrl = "http://edge-controller";
        var generateMessage = restTemplate.getForObject(controllerUrl + "/generate", String.class);
        log.info("generate edge nodes configuration: {}", generateMessage);
        var initMessage = restTemplate.getForObject(controllerUrl + "/init", String.class);
        log.info("init edge nodes: {}", initMessage);
    }

    public void run() {
        var controllerUrl = "http://edge-controller";
        var restartMessage = restTemplate.getForObject(controllerUrl + "/restart", String.class);
        log.info("restart experiment : {}", restartMessage);
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.error("{}", e.getMessage());
            }
        } while (!isTerminated());
        var stopMessage = restTemplate.getForObject(controllerUrl + "/stop", String.class);
        log.info("stop experiment: {}", stopMessage);
    }

    public boolean isTerminated() {
        var countNew = taskService.count(new QueryWrapper<Task>().eq("status", "NEW"));
        var countEnd = taskService.count(new QueryWrapper<Task>().eq("status", "END"));
        log.info("count NEW {}, count END {}", countNew, countEnd);
        return countEnd == 1L && countNew == 0L;
    }

    public double test() {
        var list = new ArrayList<Double>();
        for (int i = 1; i <= testNumber; i++) {
            run();
            double successRate;
            if (useRedundancy) {
                successRate = taskService.getRedundantSuccessRate();
            } else {
                successRate = taskService.getSuccessRate();
            }
            log.info("test {}, success rate : {}", i, successRate);
            list.add(successRate);
            taskService.remove(null);
        }
        var avg = MathUtils.avg(list);
        var std = MathUtils.std(list);
        log.info("avg success rate: {}, std success rate: {}", avg, std);
        return avg;
    }

    public void updateModelByHdfs(String flag) {
        for (int i = 1; i < edgeNodeNumber; i++) {
            String edgeNodeId = String.format("edge-node-%d", i);
            restTemplate.getForObject("http://" + edgeNodeId + "/updateModel/{flag}", String.class, Map.of("flag", flag));
        }
    }

    public void addData() {
        var teamRewards = new float[timeSlotNumber * 2];
        var states = new float[timeSlotNumber][edgeNodeNumber][stateShape];
        var actions = new int[timeSlotNumber][edgeNodeNumber][1];
        var availActions = new int[timeSlotNumber][edgeNodeNumber][offloadingShape];
        var rewards = new float[timeSlotNumber][edgeNodeNumber][1];
        var nextStates = new float[timeSlotNumber][edgeNodeNumber][stateShape];

        for (int i = 1; i <= timeSlotNumber; i++) {
            for (int j = 1; j <= edgeNodeNumber; j++) {
                var state = transitionService.getObservation(j, i);
                states[i - 1][j - 1] = state;
                var action = transitionService.getAction(j, i);
                actions[i - 1][j - 1] = action;
                var nextState = transitionService.getObservation(j, i + 1);
                nextStates[i - 1][j - 1] = nextState;
                var availAction = transitionService.getAvailAction(j, i);
                availActions[i - 1][j - 1] = availAction;

                var reward = transitionService.getReward(j, i);
                if (useRewardInTime) {
                    teamRewards[i - 1] += reward;
                } else {
                    var task = taskService.getOne(new QueryWrapper<Task>().eq("source", j).eq("time_slot", i).last("limit 1"));
                    if (task != null) {
                        long totalTime = task.getTransmissionWaitingTime() + task.getTransmissionTime() + task.getExecutionWaitingTime() + task.getExecutionTime();
                        var endTimeSlot = task.getTimeSlot() + totalTime / timeSlotLen;
                        int timeSlotIndex = (int) endTimeSlot - 1;
                        teamRewards[timeSlotIndex] += reward;
                    }
                }
            }
        }

        for (int i = 1; i <= timeSlotNumber; i++) {
            for (int j = 1; j <= edgeNodeNumber; j++) {
                rewards[i - 1][j - 1] = new float[]{teamRewards[i - 1]};
            }
        }

        for (int i = 1; i <= timeSlotNumber; i++) {
            buffer.insert(states[i - 1], actions[i - 1], availActions[i - 1], rewards[i - 1], nextStates[i - 1]);
        }
    }
}
