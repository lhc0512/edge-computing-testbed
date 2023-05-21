package cn.edu.scut;

import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestDJL {

    @Test
    public void test(){
        var manager = NDManager.newBaseManager();
        var data = manager.create(11.5f);
        log.info("{}", data);
        manager.close();
    }

    @Test
    public void testCopy(){
        var manager = NDManager.newBaseManager();
        var data = manager.create(11.5f);
        var d = data.zerosLike();
        data.copyTo(d);
        log.info("{}", d);
        manager.close();
    }


    @Test
    public void testGamma(){
        log.info("{}", Math.pow(0.99, 10));
        log.info("{}", Math.pow(0.95, 10));
    }


}
