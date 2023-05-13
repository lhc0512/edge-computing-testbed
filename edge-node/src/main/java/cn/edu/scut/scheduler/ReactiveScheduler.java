package cn.edu.scut.scheduler;

import cn.edu.scut.bean.EdgeNodeSystem;
import cn.edu.scut.bean.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Lazy
@Component
public class ReactiveScheduler implements IScheduler {

    @Value("${spring.application.name}")
    public String name;

    @Autowired
    private EdgeNodeSystem edgeNodeSystem;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    @Value("${heuristic.queue-coef}")
    private float queueCoef;

    @Autowired
    private Random schedulerRandom;

    @Override
    public int[] selectAction(Task task) {
        var id = Integer.parseInt(name.split("-")[2]);
        if (edgeNodeSystem.getExecutionQueue().getSize() < edgeNodeSystem.getExecutionQueueThreshold() * queueCoef) {
            return new int[]{id};
        }
        var edgeNodeIds = new ArrayList<Integer>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            edgeNodeIds.add(i);
        }
        var selectedNodes = new HashSet<Integer>();
        // while true
        for (int i = 0; i < 1000; i++) {
            var edgeNodeId = edgeNodeIds.get(schedulerRandom.nextInt(edgeNodeIds.size()));
            if (edgeNodeIds.size() > 2 && edgeNodeId.equals(id)) {
                continue;
            }
            selectedNodes.add(edgeNodeId);
            if (selectedNodes.size() == 2) {
                break;
            }
        }
        var queue = new PriorityQueue<>(Comparator.comparingInt((Map<String, Object> o) -> (int) o.get("queue")));
        for (Integer edgeNodeId : selectedNodes) {
            var info = new HashMap<String, Object>();
            var url = String.format("http://edge-node-%s/edgeNode/queue", edgeNodeId);
            var queueSize = restTemplate.getForObject(url, Integer.class);
            info.put("queue", queueSize);
            info.put("edgeId", edgeNodeId);
            queue.add(info);
        }
        return new int[]{(Integer) queue.poll().get("edgeId")};
    }
}