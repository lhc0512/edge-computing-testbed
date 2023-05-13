package cn.edu.scut.util;

import cn.edu.scut.bean.EdgeNode;
import cn.edu.scut.bean.Link;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.EdgeNodeService;
import cn.edu.scut.service.LinkService;
import cn.edu.scut.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EnvUtils implements InitializingBean {
    @Value("${env.max-task-redundancy}")
    private int maxTaskRedundancy;
    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    @Value("${env.use-task-reliability}")
    private boolean useTaskReliability;

    private int offloadShape;
    private int actionShape;

    @Autowired
    TaskService taskService;

    @Autowired
    EdgeNodeService edgeNodeService;

    @Autowired
    LinkService linkService;

    @Override
    public void afterPropertiesSet() {
        offloadShape = edgeNodeNumber + 1;
        actionShape = offloadShape * maxTaskRedundancy;
    }

    public void maskEdgeNode(int start, int j, int[] availAction) {
        for (int i = start; i < maxTaskRedundancy; i++) {
            availAction[i * offloadShape + j] = 0;
        }
    }

    public void maskEdgeNode(int j, int[] availAction) {
        for (int i = 0; i < maxTaskRedundancy; i++) {
            availAction[i * offloadShape + j] = 0;
        }
    }

    public boolean isMeetReliabilityRequirement(Set<Integer> set, Task task) {
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
        var reliability = 1 - unReliability;
        return reliability >= task.getReliabilityRequirement();
    }

    public int[] getRedundantAvailAction() {
        var availAction = new int[actionShape];
        for (int i = 0; i < maxTaskRedundancy; i++) {
            for (int j = 0; j < offloadShape - 1; j++) {
                var index = i * offloadShape + j;
                availAction[index] = 1;
            }
        }
        return availAction;
    }

    public int[] getNonTaskRedundantAvailAction() {
        var availAction = new int[actionShape];
        for (int i = 0; i < maxTaskRedundancy; i++) {
            var index = i * offloadShape + offloadShape - 1;
            availAction[index] = 1;
        }
        return availAction;
    }
}
