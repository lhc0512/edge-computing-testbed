package cn.edu.scut.bean;

import lombok.Data;

@Data
public class RatcVo {
    private Integer edgeId;
    private Double executionFailureRate;
    private Long capacity;
    private Integer waitingTime;
    private Integer totalTime;
}
