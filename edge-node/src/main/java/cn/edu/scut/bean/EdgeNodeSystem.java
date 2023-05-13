package cn.edu.scut.bean;

import cn.edu.scut.queue.ExecutionQueue;
import cn.edu.scut.queue.TransmissionQueue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
public class EdgeNodeSystem {
    private EdgeNode edgeNode;
    private Map<Integer, Link> linkMap;
    private ExecutionQueue executionQueue;
    private Map<Integer, TransmissionQueue> transmissionQueueMap;
    // threshold
    private float executionQueueThreshold;
}
