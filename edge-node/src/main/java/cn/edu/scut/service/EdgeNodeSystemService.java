package cn.edu.scut.service;

import cn.edu.scut.bean.*;
import cn.edu.scut.queue.ExecutionQueue;
import cn.edu.scut.queue.TransmissionQueue;
import cn.edu.scut.scheduler.*;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;

@Setter
@Getter
@Service
@Slf4j
@RefreshScope
public class EdgeNodeSystemService implements InitializingBean {

    @Value("${spring.application.name}")
    private String name;
    @Value("${env.seed}")
    private Integer seed;
    @Value("${env.cpu-capacity}")
    private Integer cpuCapacity;
    @Value("${env.scheduler}")
    private String scheduler;
    @Value("${env.min-cpu-core}")
    private int minCpuCore;
    @Value("${env.queue-coef}")
    private float queueCoef;
    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;
    private int offloadingShape;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private EdgeNodeSystem edgeNodeSystem;
    @Lazy
    @Autowired
    private DRLScheduler DRLScheduler;

    @Lazy
    @Autowired
    private RandomScheduler randomScheduler;

    @Lazy
    @Autowired
    private ReliabilityTwoChoice reliabilityTwoChoice;

    @Lazy
    @Autowired
    private ReactiveScheduler reactiveScheduler;

    @Lazy
    @Autowired
    private ESFScheduler esfScheduler;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private TaskService taskService;

    @Override
    public void afterPropertiesSet() {
        offloadingShape = edgeNodeNumber + 1;
    }

    @Async
    public void processTaskFromUser(Task task) {
        // 兼容 redundancy task
        int[] actions = switch (scheduler) {
            case "rl" -> DRLScheduler.selectAction(task);
            case "random" -> randomScheduler.selectAction(task);
            case "reactive" -> reactiveScheduler.selectAction(task);
            case "reliability-two-choice" -> reliabilityTwoChoice.selectAction(task);
            case "esf" -> esfScheduler.selectAction(task);
            default -> throw new RuntimeException("no scheduler");
        };
        for (int i = 0; i < actions.length; i++) {
            // 不做卸载决策的动作，虚拟节点 n+1
            if (actions[i] == offloadingShape) {
                if (i == 0) {
                    throw new RuntimeException("the action of the first task can not be offloading shape!!!");
                }
                break;
            }
            Task copyTask;
            // primary task
            if (i == 0) {
                copyTask = task;
            } else {
                // backup task
                copyTask = JSONObject.parseObject(JSONObject.toJSONString(task), Task.class);
                // store backup task in database
                copyTask.setId(null);
                taskService.save(copyTask);
            }
            processEachTask(copyTask, actions[i]);
        }
    }

    private void processEachTask(Task task, Integer action) {
        var id = Integer.parseInt(name.split("-")[2]);
        task.setDestination(action);
        if (action.equals(id)) {
            task.setBeginExecutionTime(LocalDateTime.now());
            task.setEndTransmissionTime(LocalDateTime.now());
            task.setTransmissionTime(0);
            task.setTransmissionWaitingTime(0);
            processTaskFromEdgeNode(task);
        } else {
            Link link = edgeNodeSystem.getLinkMap().get(action);
            double transmissionTime = task.getTaskSize() / link.getTransmissionRate() * 1000;
            task.setTransmissionTime((int) transmissionTime);
            edgeNodeSystem.getTransmissionQueueMap().get(action).add(task);
        }
    }

    public void processTaskFromEdgeNode(Task task) {
        double executionTime = task.getCpuCycle().doubleValue() / edgeNodeSystem.getEdgeNode().getCapacity().doubleValue() * 1000;
        task.setExecutionTime((int) executionTime);
        edgeNodeSystem.getExecutionQueue().add(task);
    }

    public void init() {
        // SQL
        var id = Integer.parseInt(name.split("-")[2]);
        var edgeNodeConfig = edgeNodeService.getOne(new QueryWrapper<EdgeNode>().eq("edge_node_id", id));
        EdgeNode edgeNode = new EdgeNode();
        edgeNode.setEdgeNodeId(id);
        edgeNode.setCpuNum(edgeNodeConfig.getCpuNum());
        edgeNode.setExecutionFailureRate(edgeNodeConfig.getExecutionFailureRate());
        edgeNode.setTaskRate(edgeNodeConfig.getTaskRate());
        edgeNode.setCapacity(edgeNodeConfig.getCpuNum() * Constants.Giga.value * cpuCapacity);
        // RD-DRL
        edgeNode.setEdgeNodeReliability(edgeNodeConfig.getEdgeNodeReliability());
        edgeNodeSystem.setEdgeNode(edgeNode);
        edgeNodeSystem.setExecutionQueue(new ExecutionQueue());
        log.info("edge-node-{} edge config: {}", id, edgeNode);

        var transmissionQueueMap = new HashMap<Integer, TransmissionQueue>();
        var linkMap = new HashMap<Integer, Link>();
        // SQL
        var links = linkService.list(new QueryWrapper<Link>().eq("source", id));
        for (Link link : links) {
            transmissionQueueMap.put(link.getDestination(), new TransmissionQueue());
            linkMap.put(link.getDestination(), link);
        }
        edgeNodeSystem.setTransmissionQueueMap(transmissionQueueMap);
        edgeNodeSystem.setLinkMap(linkMap);
        // availAction
        edgeNodeSystem.setExecutionQueueThreshold(Float.valueOf(edgeNode.getCpuNum()) / (float) minCpuCore * queueCoef);
        log.info("edge-node-{} link config：{}", id, links);
        log.info("load edge nodes and links configuration completed");
    }
}
