package cn.edu.cug.campuslostfound.mapper;

import cn.edu.cug.campuslostfound.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    // 查询会话内的消息列表（按时间倒序）
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId, @Param("pageNum") Integer pageNum, @Param("pageSize") Integer pageSize);
}