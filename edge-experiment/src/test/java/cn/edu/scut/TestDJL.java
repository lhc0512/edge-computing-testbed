package cn.edu.scut;

import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestDJL {

    @Test
    public void test1() {
        var manager = NDManager.newBaseManager();
        var data = manager.arange(2 * 3 * 3 * 4).reshape(new Shape(2, 3, 3, 4)).toType(DataType.FLOAT32, true);
        var index = manager.zeros(new Shape(2, 3, 3, 1), DataType.INT32);
        var gatherData = data.gather(index, -1);
        log.info("{}", gatherData);
        manager.close();
    }
}