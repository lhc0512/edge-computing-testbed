package cn.edu.scut.agent;

import java.io.InputStream;

public interface IMultiAgent {

    int selectAction(float[] state, int[] availAction, boolean training);

    // 需要结合边缘计算环境的具体信息选取动作
    int[] selectAction(float[] state, int[] availAction, boolean training, int taskId);

    void train();

    void saveModel(String flag);

    void loadModel(String flag);

    void saveHdfsModel(String flag);

    void loadHdfsModel(String flag);

    void loadSteamModel(InputStream inputStream, String fileName);
}
