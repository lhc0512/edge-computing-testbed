package cn.edu.scut.controller;

import cn.edu.scut.bean.EdgeNode;
import cn.edu.scut.bean.RatcVo;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.EdgeNodeSystemService;
import cn.edu.scut.thread.ExecutionRunnable;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/edgeNode", method = {RequestMethod.GET, RequestMethod.POST})
@CommonsLog
public class EdgeNodeController {

    @Autowired
    private EdgeNodeSystemService edgeNodeSystemService;

    @PostMapping("/task")
    public String receiveEdgeNodeTask(@RequestBody Task task) {
        log.info("receive task from edge node: " + task.getSource());
        edgeNodeSystemService.processTaskFromEdgeNode(task);
        return "success";
    }

    @GetMapping("/queue")
    public Integer queue() {
        return edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getSize();
    }

    @GetMapping("/info")
    public EdgeNode info() {
        return edgeNodeSystemService.getEdgeNodeSystem().getEdgeNode();
    }

    @GetMapping("/waitingTime")
    public int waitingTime() {
        var queue = edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getExecutor().getQueue();
        int waitingTime = 0;
        for (Runnable runnable : queue) {
            var r = (ExecutionRunnable) runnable;
            waitingTime += r.getTask().getExecutionTime();
        }
        return waitingTime;
    }

    @GetMapping("/ratc")
    public RatcVo ratc() {
        var queue = edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getExecutor().getQueue();
        int waitingTime = 0;
        for (Runnable runnable : queue) {
            var r = (ExecutionRunnable) runnable;
            waitingTime += r.getTask().getExecutionTime();
        }
        var res = new RatcVo();
        res.setWaitingTime(waitingTime);
        var edgeNode = edgeNodeSystemService.getEdgeNodeSystem().getEdgeNode();
        res.setExecutionFailureRate(edgeNode.getExecutionFailureRate());
        res.setCapacity(edgeNode.getCapacity());
        res.setEdgeId(edgeNode.getEdgeNodeId());
        return res;
    }

    @GetMapping("/available")
    public Integer avail() {
        int queueSize = edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueue().getSize();
        if (queueSize < edgeNodeSystemService.getEdgeNodeSystem().getExecutionQueueThreshold()) {
            return 1;
        } else {
            return 0;
        }
    }
}