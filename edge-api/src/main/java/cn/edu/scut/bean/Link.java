package cn.edu.scut.bean;

import lombok.Data;

@Data
public class Link {
    private Integer id;
    private Integer source;
    private Integer destination;
    private Double transmissionRate;
    private Double transmissionFailureRate;
    // RD-DRL
    private Double linkReliability;
}
