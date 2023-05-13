package cn.edu.scut;

import cn.edu.scut.util.RunningPoint;
import cn.edu.scut.util.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class ExperimentApp {
    public static void main(String[] args) {
        var context = SpringApplication.run(ExperimentApp.class, args);
        SpringBeanUtils.setApplicationContext(context);
        var runningPoint =  context.getBean(RunningPoint.class);
        // waiting for edge-nodes and edge-controller start.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        runningPoint.run();
    }

    // log日志存储当前实验的所有配置，方便复现
//    @EventListener
//    public void handleContextRefresh(ContextRefreshedEvent event) {
//        final Environment env = event.getApplicationContext().getEnvironment();
//        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
//        StreamSupport.stream(sources.spliterator(), false)
//                .filter(ps -> ps instanceof EnumerablePropertySource)
//                .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
//                .flatMap(Arrays::stream)
//                .distinct()
//                .filter(prop  -> prop.contains("rl.") || prop.contains("env."))
//                .forEach(prop -> log.info("{}: {}", prop, env.getProperty(prop)));
//    }

}