package cn.edu.cug.campuslostfound.mapper;

import cn.edu.cug.campuslostfound.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// ChatSessionMapper.java
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    // 自定义查询：根据两个用户ID查询会话（保证user1<user2避免重复）
    @Select("SELECT * FROM chat_session WHERE (user1_id = #{userId1} AND user2_id = #{userId2}) OR (user1_id = #{userId2} AND user2_id = #{userId1})")
    ChatSession selectByUserPair(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
