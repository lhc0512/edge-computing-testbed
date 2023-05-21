package cn.edu.scut.thread;

import cn.edu.scut.bean.EdgeNodeSystem;
import cn.edu.scut.bean.Task;
import cn.edu.scut.bean.TaskStatus;
import cn.edu.scut.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@Setter
@Scope("prototype")
public class ExecutionRunnable implements Runnable {
    @Value("${env.use-poisson-reliability}")
    private boolean usePoissonReliability;

    @Value("${env.use-constant-reliability}")
    private boolean useConstantReliability;

    @Value("${env.use-task-reliability}")
    private boolean useTaskReliability;
    @Getter
    private Task task;

    @Autowired
    private TaskService taskService;
    @Autowired
    private EdgeNodeSystem edgeNodeSystem;
    @Autowired
    private Random reliabilityRandom;

    @Override
    public void run() {
        task.setBeginExecutionTime(LocalDateTime.now());
        task.setExecutionWaitingTime((int) Duration.between(task.getEndTransmissionTime(), task.getBeginExecutionTime()).toMillis());
        var successTask = taskService.getOne(new QueryWrapper<Task>().eq("job_id", task.getJobId()).eq("status", "SUCCESS").last("limit 1"));
        // 已经有执行成功的，不再执行
        if (successTask != null) {
            task.setStatus(TaskStatus.CANCEL);
            taskService.updateById(task);
            return;
        }
        // drop the task without wasting the resource.
        int estimatedTotalTime = task.getTransmissionWaitingTime() + task.getTransmissionTime() + task.getExecutionWaitingTime() + task.getExecutionTime();
        if (estimatedTotalTime > task.getDeadline()) {
            task.setStatus(TaskStatus.DROP);
            taskService.updateById(task);
            return;
        }
        try {
            Thread.sleep(task.getExecutionTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        task.setEndExecutionTime(LocalDateTime.now());
        // reliability
        var isFailure = false;
        if (usePoissonReliability) {
            double reliability = Math.exp(-task.getExecutionTime() / 1000.0 * edgeNodeSystem.getEdgeNode().getExecutionFailureRate());
            if (reliabilityRandom.nextDouble() > reliability) {
                isFailure = true;
            }
        } else if (useConstantReliability) {
            if (useTaskReliability) {
                if (reliabilityRandom.nextDouble() > edgeNodeSystem.getEdgeNode().getEdgeNodeReliability() * task.getTaskReliability()) {
                    isFailure = true;
                }
            } else {
                if (reliabilityRandom.nextDouble() > edgeNodeSystem.getEdgeNode().getEdgeNodeReliability()) {
                    isFailure = true;
                }
            }
        }

        if (isFailure) {
            task.setStatus(TaskStatus.EXECUTION_FAILURE);
        } else {
            task.setStatus(TaskStatus.SUCCESS);
        }
        taskService.updateById(task);
    }
}
