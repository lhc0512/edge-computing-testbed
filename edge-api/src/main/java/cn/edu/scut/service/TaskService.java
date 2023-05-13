package cn.edu.scut.service;

import cn.edu.scut.bean.Task;
import cn.edu.scut.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class TaskService extends ServiceImpl<TaskMapper, Task> {

    @Autowired
    private TaskMapper taskMapper;

    public double getSuccessRate() {
        long successTasks = taskMapper.selectCount(new QueryWrapper<Task>().eq("status", "SUCCESS"));
        // 有一个多余的END
        long totalTasks = taskMapper.selectCount(new QueryWrapper<Task>()) - 1;
        return (double) successTasks / (double) totalTasks;
    }

    public double getRedundantSuccessRate() {
        int num = taskMapper.getSuccessJobNumber();
        long maxJobId = taskMapper.selectOne(new QueryWrapper<Task>().orderByDesc("job_id").last("limit 1")).getJobId();
        return Double.parseDouble(num + "") / maxJobId;
    }
}
