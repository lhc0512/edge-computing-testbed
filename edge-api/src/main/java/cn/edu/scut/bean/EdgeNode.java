package cn.edu.scut.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class EdgeNode {
    // database
    private Integer id;
    // edge node
    private Integer edgeNodeId;
    private Long cpuNum;
    @TableField(exist = false)
    private Long capacity; //  capacity = cpuNum * cpuCapacity
    private Double taskRate;
    private Double executionFailureRate;
    // RD-DRL
    private Double edgeNodeReliability;
}