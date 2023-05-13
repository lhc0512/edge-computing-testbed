package cn.edu.scut.thread;

import cn.edu.scut.bean.EdgeNode;
import cn.edu.scut.bean.Task;
import cn.edu.scut.bean.TaskStatus;
import cn.edu.scut.service.DataGenerationService;
import cn.edu.scut.service.EdgeNodeService;
import cn.edu.scut.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Setter
@Slf4j
@Scope("prototype")
@Component
public class UserRunnable implements Runnable {

    @Value("${env.time-slot-number}")
    private int timeSlotNumber;
    private int timeSlot = 0;
    private int jobId = 0;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RandomGenerator taskRandomGenerator;
    @Autowired
    private DataGenerationService dataGenerationService;
    @Autowired
    private EdgeNodeService edgeNodeService;
    @Autowired
    private TaskService taskService;

    @Override
    public void run() {
        timeSlot += 1;
        // RL next state
        if (timeSlot > timeSlotNumber + 1) {
            var endTask = taskService.getOne(new QueryWrapper<Task>().eq("status", "END"));
            if (endTask != null) {
                return;
            }
            var task = new Task();
            task.setStatus(TaskStatus.END);
            taskService.save(task);
            return;
        }
        try {
            List<EdgeNode> edgeNodes = edgeNodeService.list();
            for (EdgeNode edgeNode : edgeNodes) {
                boolean flag = taskRandomGenerator.nextDouble() < edgeNode.getTaskRate();
                if (flag) {
                    var task = dataGenerationService.generateTask(edgeNode.getEdgeNodeId());
                    task.setTimeSlot(timeSlot);
                    task.setJobId(++jobId);
                    taskService.save(task);
                    String url = String.format("http://edge-node-%s/user/task", edgeNode.getEdgeNodeId());
                    restTemplate.postForObject(url, task, String.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}