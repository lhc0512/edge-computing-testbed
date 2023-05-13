package cn.edu.scut.thread;

import cn.edu.scut.bean.EdgeNodeSystem;
import cn.edu.scut.bean.Link;
import cn.edu.scut.bean.Task;
import cn.edu.scut.bean.TaskStatus;
import cn.edu.scut.service.TaskService;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@Scope("prototype")
@Setter
@RefreshScope
@CommonsLog
public class TransmissionRunnable implements Runnable {
    // prototype
    private Task task;
    @Value("${env.use-poisson-reliability}")
    private boolean usePoissonReliability;

    @Value("${env.use-constant-reliability}")
    private boolean useConstantReliability;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;
    @Autowired
    private Random reliabilityRandom;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private TaskService taskService;

    @Override
    public void run() {
        task.setBeginTransmissionTime(LocalDateTime.now());
        task.setTransmissionWaitingTime((int) Duration.between(task.getArrivalTime(), task.getBeginTransmissionTime()).toMillis());
        try {
            Thread.sleep(task.getTransmissionTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        task.setEndTransmissionTime(LocalDateTime.now());
        int destination = task.getDestination();
        var link = edgeNodeSystem.getLinkMap().get(destination);

        // reliability
        var isFailure = false;
        if (usePoissonReliability) {
            double transmissionFailureRate = link.getTransmissionFailureRate();
            double reliability = Math.exp(-task.getTransmissionTime() / 1000.0 * transmissionFailureRate);
            if (reliabilityRandom.nextDouble() > reliability) {
                isFailure = true;
            }
        } else if (useConstantReliability) {
            if (reliabilityRandom.nextDouble() > link.getLinkReliability()) {
                isFailure = true;
            }
        } else {
            throw new RuntimeException("error in reliability model!");
        }

        if (isFailure) {
            task.setStatus(TaskStatus.TRANSMISSION_FAILURE);
            // fixed null type exception: we will calculate reward based on the total time.
            task.setExecutionWaitingTime(0);
            task.setExecutionTime(0);
            taskService.updateById(task);
        } else {
            String url = String.format("http://edge-node-%s/edgeNode/task", destination);
            restTemplate.postForObject(url, task, String.class);
        }
    }
}
