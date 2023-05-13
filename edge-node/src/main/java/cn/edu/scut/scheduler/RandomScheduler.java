package cn.edu.scut.scheduler;

import cn.edu.scut.bean.EdgeNode;
import cn.edu.scut.bean.Link;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.EdgeNodeService;
import cn.edu.scut.service.LinkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@Lazy
@Component
@Setter
public class RandomScheduler implements IScheduler {

    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    @Value("${env.use-redundancy}")
    private boolean useRedundancy;

    @Value("${env.max-task-redundancy}")
    private int maxTaskRedundancy;

    @Value("${env.use-task-reliability}")
    private boolean useTaskReliability;

    @Autowired
    private Random schedulerRandom;

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private LinkService linkService;

    @Override
    public int[] selectAction(Task task) {
        if (useRedundancy) {
            return selectMultiAction(task);
        } else {
            return selectSingleAction(task);
        }
    }

    private int[] selectSingleAction(Task task) {
        var services = new ArrayList<Integer>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            services.add(i);
        }
        int index = schedulerRandom.nextInt(services.size());
        return new int[]{services.get(index)};
    }

    private int[] selectMultiAction(Task task) {
        var res = new ArrayList<Integer>();
        var list = new ArrayList<Integer>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            list.add(i);
        }
        Collections.shuffle(list, schedulerRandom);
        for (int i = 0; i < maxTaskRedundancy; i++) {
            Integer a = list.get(i);
            res.add(a);
            var unReliability = 1.0f;
            for (Integer s : res) {
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
        // 补全动作空间维度
        for (int i = res.size() + 1; i <= maxTaskRedundancy; i++) {
            var action = edgeNodeNumber + 1;
            res.add(action);
        }
        return res.stream().mapToInt(x -> x).toArray();
    }
}