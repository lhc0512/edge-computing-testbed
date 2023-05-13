package cn.edu.scut;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestMath {

    @Test
    public void test(){
        var taskRandomGenerator=  new JDKRandomGenerator();
        var taskComplexity = new UniformIntegerDistribution(taskRandomGenerator, 500, 500);
        for (int i = 0; i < 10; i++) {
            log.info("task complexity = {}", taskComplexity.sample());
        }
    }
}
