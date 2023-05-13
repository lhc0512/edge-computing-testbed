package cn.edu.scut.controller;

import cn.edu.scut.bean.Task;
import cn.edu.scut.service.EdgeNodeSystemService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping(value = "/user", method = {RequestMethod.GET, RequestMethod.POST})
@CommonsLog
public class UserController {
    @Autowired
    EdgeNodeSystemService edgeNodeSystemService;

//    @Value("${env.use-redundancy}")
//    private boolean useRedundancy;

    @PostMapping("/task")
    public String receiveUserTask(@RequestBody Task task) {
        log.info("receive task from user");
        task.setArrivalTime(LocalDateTime.now());
        edgeNodeSystemService.processTaskFromUser(task);
//        if (useRedundancy) {
//            edgeNodeSystemService.processRedundantTaskFromUser(task);
//        } else {
//            edgeNodeSystemService.processTaskFromUser(task);
//        }
        return "success";
    }
}
