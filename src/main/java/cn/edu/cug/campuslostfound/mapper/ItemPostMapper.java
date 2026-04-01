package cn.edu.cug.campuslostfound.mapper;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring Boot 这是一个 Mapper 接口
public interface ItemPostMapper extends BaseMapper<ItemPost> {
}
