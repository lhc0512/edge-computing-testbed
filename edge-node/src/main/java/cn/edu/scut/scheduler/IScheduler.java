package cn.edu.scut.scheduler;

import cn.edu.scut.bean.Task;

public interface IScheduler {
    int[] selectAction(Task task);
}
