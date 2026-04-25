package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ChatMessage; // 必须加
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {
    ChatMessage sendMessage(Long senderId, Long receiverId, String content);
    List<ChatMessage> getMessageBySessionId(Long sessionId, Integer pageNum, Integer pageSize);
    void markMessageAsRead(Long msgId);
}