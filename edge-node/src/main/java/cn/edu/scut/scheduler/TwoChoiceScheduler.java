package cn.edu.scut.scheduler;

import cn.edu.scut.bean.EdgeNode;
import cn.edu.scut.bean.Link;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.EdgeNodeService;
import cn.edu.scut.service.LinkService;
import cn.edu.scut.util.EnvUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Lazy
@Component
/**
 * DR-DRL
 */
public class TwoChoiceScheduler implements IScheduler, InitializingBean {
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

    @Autowired
    private Random schedulerRandom;

    @Autowired
    private EnvUtils envUtils;

    @Override
    public void afterPropertiesSet() throws Exception {
        offloadingShape = edgeNodeNumber + 1;
    }

    @Override
    public int[] selectAction(Task task) {
        var actions = new int[maxTaskRedundancy];
        Arrays.fill(actions, offloadingShape);

        var candidateEdgeNode = new ArrayList<Integer>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            candidateEdgeNode.add(i);
        }
        var set = new HashSet<Integer>();
        for (int i = 0; i < maxTaskRedundancy; i++) {
            var priority = new ArrayList<Entity>();
            for (int j = 0; j < 2; j++) {
                var a1 = schedulerRandom.nextInt(candidateEdgeNode.size());
                var a = candidateEdgeNode.remove(a1);
                var time = restTemplate.getForObject("http://edge-node-" + a + "/edgeNode/waitingTime", Integer.class);
                priority.add(new Entity(time, a));
            }
            priority.sort(Comparator.comparing(e -> e.time));

            actions[i] = priority.get(0).edgeNodeId;
            set.add(priority.get(0).edgeNodeId);
            candidateEdgeNode.add(priority.get(1).edgeNodeId);

            if (envUtils.isMeetReliabilityRequirement(set, task)) {
                break;
            }
        }
        log.info("actions :{}", actions);
        return actions;
    }

    @Data
    @AllArgsConstructor
    public static class Entity {
        public int time;
        public int edgeNodeId;
    }
}
