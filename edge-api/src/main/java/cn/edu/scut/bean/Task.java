package cn.edu.scut.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Task {
    private Integer id;
    // RD-DRL 多个冗余任务，id不同，但同一个jobId
    private Integer jobId;
    private Integer timeSlot;
    // JSON string, to support array
    private Integer source;
    private Integer destination;
    private String action; // 包含所有的destination
    private TaskStatus status;
    // KB
    private Long taskSize;
    private Integer taskComplexity;
    // cycle
    private Long cpuCycle;  // cpuCycle = taskSize * taskComplexity
    // s
    private Integer deadline;
    private Integer transmissionTime;
    private Integer executionTime;
    private Integer transmissionWaitingTime;
    private Integer executionWaitingTime;

    @TableField(exist = false)
    private LocalDateTime arrivalTime;
    @TableField(exist = false)
    private LocalDateTime beginTransmissionTime;
    @TableField(exist = false)
    private LocalDateTime endTransmissionTime;
    @TableField(exist = false)
    private LocalDateTime beginExecutionTime;
    @TableField(exist = false)
    private LocalDateTime endExecutionTime;
    @TableField(exist = false)
    private Double transmissionFailureRate;
    @TableField(exist = false)
    private Double executionFailureRate;
    // JSON string, to support array
    private String availAction;
    // RD-DRL
    private Double taskReliability;
    private Double reliabilityRequirement;
    @TableField(exist = false)
    private Double executionReliability;
    @TableField(exist = false)
    private Double transmissionReliability;

    private String runtimeInfo;
}
