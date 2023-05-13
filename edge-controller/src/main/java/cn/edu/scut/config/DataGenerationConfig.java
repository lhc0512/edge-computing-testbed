package cn.edu.scut.config;

import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataGenerationConfig {

    @Value("${env.min-execution-failure-rate}")
    private double minExecutionFailureRate;
    @Value("${env.max-execution-failure-rate}")
    private double maxExecutionFailureRate;

    @Value("${env.min-cpu-core}")
    private int minCpuCore;
    @Value("${env.max-cpu-core}")
    private int maxCpuCore;

    @Value("${env.min-task-rate}")
    private double minTaskRate;
    @Value("${env.max-task-rate}")
    private double maxTaskRate;

    @Value("${env.min-transmission-rate}")
    private double minTransmissionRate;
    @Value("${env.max-transmission-rate}")
    private double maxTransmissionRate;

    @Value("${env.min-transmission-failure-rate}")
    private double minTransmissionFailureRate;
    @Value("${env.max-transmission-failure-rate}")
    private double maxTransmissionFailureRate;

    @Value("${env.edge-node-seed}")
    private int edgeNodeSeed;
    @Value("${env.task-seed}")
    private int taskSeed;

    @Value("${env.min-task-size}")
    private int minTaskSize;
    @Value("${env.max-task-size}")
    private int maxTaskSize;

    @Value("${env.min-task-complexity}")
    private int minTaskComplexity;
    @Value("${env.max-task-complexity}")
    private int maxTaskComplexity;

    @Value("${env.min-transmission-reliability}")
    private double minTransmissionReliability;
    @Value("${env.max-transmission-reliability}")
    private double maxTransmissionReliability;


    @Value("${env.min-execution-reliability}")
    private double minExecutionReliability;
    @Value("${env.max-execution-reliability}")
    private double maxExecutionReliability;

    @Value("${env.min-task-reliability}")
    private float minTaskReliability;
    @Value("${env.max-task-reliability}")
    private float maxTaskReliability;
    @Value("${env.min-task-reliability-requirement}")
    private float minTaskReliabilityRequirement;
    @Value("${env.max-task-reliability-requirement}")
    private float maxTaskReliabilityRequirement;

    @Bean
    public RandomGenerator randomGenerator() {
        var random = new JDKRandomGenerator();
        random.setSeed(edgeNodeSeed);
        return random;
    }

    @Bean
    public UniformRealDistribution executionFailureRandom(RandomGenerator randomGenerator) {
        return new UniformRealDistribution(randomGenerator, minExecutionFailureRate, maxExecutionFailureRate);
    }

    @Bean
    public UniformIntegerDistribution cpuCoreRandom(RandomGenerator randomGenerator) {
        return new UniformIntegerDistribution(randomGenerator, minCpuCore / 4, maxCpuCore / 4);
    }

    @Bean
    public UniformRealDistribution taskRateRandom(RandomGenerator randomGenerator) {
        return new UniformRealDistribution(randomGenerator, minTaskRate, maxTaskRate);
    }

    @Bean
    public UniformRealDistribution transmissionRateRandom(RandomGenerator randomGenerator) {
        return new UniformRealDistribution(randomGenerator, minTransmissionRate, maxTransmissionRate);
    }

    @Bean
    public UniformRealDistribution transmissionFailureRateRandom(RandomGenerator randomGenerator) {
        return new UniformRealDistribution(randomGenerator, minTransmissionFailureRate, maxTransmissionFailureRate);
    }

    @Bean
    public UniformRealDistribution executionReliabilityRandom(RandomGenerator randomGenerator) {
        return new UniformRealDistribution(randomGenerator, minExecutionReliability, maxExecutionReliability);
    }

    @Bean
    public UniformRealDistribution transmissionReliabilityRandom(RandomGenerator randomGenerator) {
        return new UniformRealDistribution(randomGenerator, minTransmissionReliability, maxTransmissionReliability);
    }

    @Bean
    public RandomGenerator taskRandomGenerator() {
        var random = new JDKRandomGenerator();
        random.setSeed(taskSeed);
        return random;
    }

    @Bean
    public UniformIntegerDistribution taskSizeRandom(RandomGenerator taskRandomGenerator) {
        return new UniformIntegerDistribution(taskRandomGenerator, minTaskSize, maxTaskSize);
    }

    @Bean
    public UniformIntegerDistribution taskComplexityRandom(RandomGenerator taskRandomGenerator) {
        return new UniformIntegerDistribution(taskRandomGenerator, minTaskComplexity, maxTaskComplexity);
    }

    @Bean
    public UniformRealDistribution taskReliabilityRandom(RandomGenerator taskRandomGenerator) {
        return new UniformRealDistribution(taskRandomGenerator, minTaskReliability, maxTaskReliability);
    }

    @Bean
    public UniformRealDistribution taskReliabilityRequirementRandom(RandomGenerator taskRandomGenerator) {
        return new UniformRealDistribution(taskRandomGenerator, minTaskReliabilityRequirement, maxTaskReliabilityRequirement);
    }
}
