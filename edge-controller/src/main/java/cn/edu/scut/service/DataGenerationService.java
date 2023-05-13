package cn.edu.scut.service;

import cn.edu.scut.bean.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;


/**
 * data generation 和 Scheduler的 random要区分
 */
@Service
@Slf4j
@RefreshScope
public class DataGenerationService {
    @Value("${env.min-transmission-failure-rate}")
    private double minTransmissionFailureRate;
    @Value("${env.max-transmission-rate}")
    private double maxTransmissionRate;
    @Value("${env.deadline}")
    private int deadline;
    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;
    @Value("${env.max-transmission-reliability}")
    private double maxTransmissionReliability;

    @Autowired
    private UniformRealDistribution executionFailureRandom;

    @Autowired
    private UniformIntegerDistribution cpuCoreRandom;

    @Autowired
    private UniformRealDistribution taskRateRandom;

    @Autowired
    private UniformRealDistribution transmissionRateRandom;

    @Autowired
    private UniformRealDistribution transmissionFailureRateRandom;

    @Autowired
    private UniformIntegerDistribution taskSizeRandom;

    @Autowired
    private UniformIntegerDistribution taskComplexityRandom;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private UniformRealDistribution executionReliabilityRandom;
    @Autowired
    private UniformRealDistribution transmissionReliabilityRandom;
    @Autowired
    private UniformRealDistribution taskReliabilityRandom;
    @Autowired
    private UniformRealDistribution taskReliabilityRequirementRandom;

    public void generateEdgeLink() {
        for (int i = 1; i <= edgeNodeNumber; i++) {
            EdgeNode edgeNode = new EdgeNode();
            edgeNode.setEdgeNodeId(i);
            edgeNode.setExecutionFailureRate(executionFailureRandom.sample());
            // reliable edge computing
            edgeNode.setEdgeNodeReliability(executionReliabilityRandom.sample());
            edgeNode.setCpuNum(cpuCoreRandom.sample() * 4L);
            edgeNode.setTaskRate(taskRateRandom.sample());
            edgeNodeService.save(edgeNode);
            for (int j = 1; j <= edgeNodeNumber; j++) {
                Link link = new Link();
                link.setSource(i);
                link.setDestination(j);
                if (i == j) {
                    link.setTransmissionRate(maxTransmissionRate * Constants.Mega.value * Constants.Byte.value);
                    link.setTransmissionFailureRate(minTransmissionFailureRate);
                    // reliable edge computing
                    link.setLinkReliability(maxTransmissionReliability);
                } else {
                    link.setTransmissionRate(transmissionRateRandom.sample() * Constants.Mega.value * Constants.Byte.value);
                    link.setTransmissionFailureRate(transmissionFailureRateRandom.sample());
                    // reliable edge computing
                    link.setLinkReliability(transmissionReliabilityRandom.sample());
                }
                linkService.save(link);
            }
        }
    }

    public Task generateTask(Integer edgeNodeId) {
        Task task = new Task();
        task.setTaskSize(((long) taskSizeRandom.sample()) * StoreConstants.Kilo.value * StoreConstants.Byte.value);
        task.setTaskComplexity(taskComplexityRandom.sample());
        task.setCpuCycle(task.getTaskComplexity() * task.getTaskSize());
        task.setDeadline(deadline);
        task.setSource(edgeNodeId);
        task.setStatus(TaskStatus.NEW);
        // reliable edge computing
        task.setTaskReliability(taskReliabilityRandom.sample());
        task.setReliabilityRequirement(taskReliabilityRequirementRandom.sample());
        return task;
    }
}