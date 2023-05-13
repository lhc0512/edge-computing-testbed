package cn.edu.scut;

import cn.edu.scut.bean.Task;
import cn.edu.scut.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestMybatisPlus {
    @Autowired
    TaskService taskService;

    @Test
    public void testCopyTask(){
        var task = new Task();
        task.setTimeSlot(11);
        taskService.save(task);
        task.setId(null);
        taskService.save(task);
    }
}
