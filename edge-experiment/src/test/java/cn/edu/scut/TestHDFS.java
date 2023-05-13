package cn.edu.scut;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@Slf4j
@SpringBootTest
public class TestHDFS {
    @Autowired
    private FileSystem fileSystem;
    @Test
    public void test() throws IOException {
//       log.info("file system {}", fileSystem);
        fileSystem.create(new Path("/demo"));

    }
}
