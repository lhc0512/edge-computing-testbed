package cn.edu.scut.mapper;

import cn.edu.scut.bean.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("""
            SELECT COUNT(DISTINCT job_id) AS SUCCESS
            FROM ec_task
            WHERE status = 'SUCCESS';
            """)
    int getSuccessJobNumber();
}
