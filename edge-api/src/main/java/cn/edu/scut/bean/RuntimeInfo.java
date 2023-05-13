package cn.edu.scut.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuntimeInfo {
    private int queueSize;
    private int waitingTime;
}
