package cn.edu.scut.controller;

import cn.edu.scut.agent.IMultiAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ModelController {

    // heuristic
    @Autowired(required = false)
    private IMultiAgent agent;

    @GetMapping("/updateModel/{flag}")
    public String updateModel(@PathVariable("flag") String flag) {
        agent.loadHdfsModel(flag);
        return "success";
    }
}