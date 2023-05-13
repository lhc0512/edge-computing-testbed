package cn.edu.scut.scheduler;

import cn.edu.scut.bean.EdgeNode;
import cn.edu.scut.bean.Link;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.EdgeNodeService;
import cn.edu.scut.service.LinkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

@Component
public class ESFScheduler implements IScheduler, InitializingBean {

    @Value("${env.use-redundancy}")
    private boolean useRedundancy;

    @Value("${env.max-task-redundancy}")
    private int maxTaskRedundancy;

    @Value("${env.use-task-reliability}")
    private boolean useTaskReliability;

    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    private int offloadingShape;
    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private RestTemplate restTemplate;


    @Override
    public void afterPropertiesSet() throws Exception {
        offloadingShape = edgeNodeNumber + 1;
    }

    @Override
    public int[] selectAction(Task task) {
        var actions = new int[maxTaskRedundancy];
        //  no offloading
        Arrays.fill(actions, offloadingShape);
        if (task == null) {
            return actions;
        }
        var entities = new ArrayList<Entity>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            var time1 = restTemplate.getForObject("http://edge-node-" + i + "/edgeNode/waitingTime", Integer.class);
            entities.add(new Entity(time1, i));
        }
        // 按照最早执行时间从小到大
        entities.sort(Comparator.comparing(e -> e.time));
        var set = new HashSet<Integer>();
        for (int i = 0; i < maxTaskRedundancy; i++) {
            int selectId = entities.get(i).edgeNodeId;
            actions[i] = selectId;
            set.add(selectId);
            var unReliability = 1.0f;
            for (Integer s : set) {
                var taskReliability = task.getTaskReliability();
                var linkReliability = linkService.getOne(new QueryWrapper<Link>().eq("source", task.getSource()).eq("destination", s)).getLinkReliability();
                var edgeNodeReliability = edgeNodeService.getOne(new QueryWrapper<EdgeNode>().eq("edge_node_id", s)).getEdgeNodeReliability();
                if (useTaskReliability) {
                    unReliability *= 1 - taskReliability * linkReliability * edgeNodeReliability;
                } else {
                    unReliability *= 1 - linkReliability * edgeNodeReliability;
                }
            }
            if ((1 - unReliability) >= task.getReliabilityRequirement()) {
                break;
            }
        }
        return actions;
    }

    @Data
    @AllArgsConstructor
    public static class Entity {
        private int time;
        private int edgeNodeId;
    }
}
