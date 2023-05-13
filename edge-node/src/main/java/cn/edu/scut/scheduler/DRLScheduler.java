package cn.edu.scut.scheduler;


import cn.edu.scut.agent.IMultiAgent;
import cn.edu.scut.bean.Task;
import cn.edu.scut.service.TaskService;
import cn.edu.scut.service.TransitionService;
import cn.edu.scut.util.EnvUtils;
import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "rl.name")
public class DRLScheduler implements IScheduler {

    // heuristic method
    @Autowired(required = false)
    private IMultiAgent agent;

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private TaskService taskService;
    @Autowired
    private EnvUtils envUtils;

    @Override
    public int[] selectAction(Task task) {
        var availAction = envUtils.getRedundantAvailAction();
        task.setAvailAction(JSONArray.toJSONString(availAction));
        var state = transitionService.getObservation(task.getSource(), task.getTimeSlot());
        int[] actions = agent.selectAction(state, availAction, false, task.getId());
        task.setAction(JSONArray.toJSONString(actions));
        taskService.updateById(task);
        return actions;
    }
}