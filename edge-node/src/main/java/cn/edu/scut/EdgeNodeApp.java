package cn.edu.scut;

import cn.edu.scut.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;
import java.util.stream.StreamSupport;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@Slf4j
public class EdgeNodeApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(EdgeNodeApp.class, args);
        SpringBeanUtils.setApplicationContext(context);
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