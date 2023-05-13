package cn.edu.scut.scheduler;

import cn.edu.scut.bean.RatcVo;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

@Slf4j
@Lazy
@Component
public class ReliabilityTwoChoice implements IScheduler {

    @Autowired
    private Random schedulerRandom;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${env.edge-node-number}")
    private int edgeNodeNumber;

    @Override
    public int[] selectAction(Task task) {
        var edgeNodeIds = new ArrayList<Integer>();
        for (int i = 1; i <= edgeNodeNumber; i++) {
            edgeNodeIds.add(i);
        }
        var selectedNodes = new HashSet<Integer>();
        // while true
        for (int i = 0; i < 1000; i++) {
            selectedNodes.add(edgeNodeIds.get(schedulerRandom.nextInt(edgeNodeIds.size())));
            if (selectedNodes.size() == 2) {
                break;
            }
        }

        var selectEdgeNodeInfo = new ArrayList<RatcVo>();
        for (Integer edgeNodeId : selectedNodes) {
            var url1 = String.format("http://edge-node-%s/edgeNode/ratc", edgeNodeId);
            var ratcVo = restTemplate.getForObject(url1, RatcVo.class);
            selectEdgeNodeInfo.add(ratcVo);
        }

        boolean flag = true;
        for (RatcVo ratcVo : selectEdgeNodeInfo) {
            int executionTime = (int) ((double) task.getCpuCycle() / ratcVo.getCapacity() * 1000);
            ratcVo.setTotalTime(executionTime + ratcVo.getWaitingTime());
            if (ratcVo.getTotalTime() > task.getDeadline()) {
                flag = false;
            }
        }
        if (flag) {
            var failureRate1 = selectEdgeNodeInfo.get(0).getExecutionFailureRate();
            var failureRate2 = selectEdgeNodeInfo.get(1).getExecutionFailureRate();
            if (failureRate1 < failureRate2) {
                return new int[]{selectEdgeNodeInfo.get(0).getEdgeId()};
            } else {
                return  new int[]{selectEdgeNodeInfo.get(1).getEdgeId()};
            }
        } else {
            var time1 = selectEdgeNodeInfo.get(0).getTotalTime();
            var time2 = selectEdgeNodeInfo.get(1).getTotalTime();
            if (time1 < time2) {
                return new int[]{selectEdgeNodeInfo.get(0).getEdgeId()};
            } else {
                return new int[]{selectEdgeNodeInfo.get(1).getEdgeId()};
            }
        }
    }
}
