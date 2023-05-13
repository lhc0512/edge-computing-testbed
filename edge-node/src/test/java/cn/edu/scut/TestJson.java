package cn.edu.scut;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class TestJson {

    @Test
    public void test() {
        Gson gson = new Gson();
        var destination = new String[]{"edge-node-1", "edge-node-2", "edge-node-3"};
        String s = gson.toJson(destination);
//        var destination2 = new String[3];
        log.info("{}", s);
        //  https://blog.csdn.net/qq_38013013/article/details/78041034
        var destination2 = gson.fromJson(s, new TypeToken<List<String>>() {
        }.getType());
        log.info("{}", destination2);
    }


    @Test
    public void test2() {
        Gson gson = new Gson();
        var destination = new String[][]{{"edge-node-1", "edge-node-2", "edge-node-3"}, {"edge-node-1", "edge-node-2", "edge-node-3"}};
        String s = gson.toJson(destination);
//        var destination2 = new String[3];
        log.info("{}", s);
        //  https://blog.csdn.net/qq_38013013/article/details/78041034
        var destination2 = gson.fromJson(s, new TypeToken<List<List<String>>>() {
        }.getType());
        log.info("{}", destination2);
    }
}
