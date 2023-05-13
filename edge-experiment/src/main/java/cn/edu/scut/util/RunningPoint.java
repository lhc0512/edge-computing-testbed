package cn.edu.scut.util;

import cn.edu.scut.runner.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class RunningPoint implements IRunner {

    @Lazy
    @Autowired
    private OnlineMARLTrainingRunner onlineMARLTrainingRunner;

    @Lazy
    @Autowired
    private OfflineMARLTrainingRunner offlineMARLTrainingRunner;

    @Lazy
    @Autowired
    private OnlineMARLTestingRunner onlineMARLTestingRunner;

    @Lazy
    @Autowired
    private OnlineHeuristicTestRunner onlineHeuristicTestRunner;

    @Lazy
    @Autowired
    private OnlineHeuristicDataRunner onlineHeuristicDataRunner;

    @Value("${env.runner}")
    private String runningType;

    @Override
    public void run() {
        switch (runningType) {
            case "rl-online" -> onlineMARLTrainingRunner.run();
            case "rl-offline" -> offlineMARLTrainingRunner.run();
            case "rl-test" -> onlineMARLTestingRunner.run();
            case "heuristic" -> onlineHeuristicTestRunner.run();
            case "heuristic-data" -> onlineHeuristicDataRunner.run();
            default -> throw new RuntimeException("error in runner type.");
        }
    }
}
