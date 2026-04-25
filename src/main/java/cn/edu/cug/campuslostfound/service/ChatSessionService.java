package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ChatSession;
import cn.edu.cug.campuslostfound.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

// ChatSessionService.java
public interface ChatSessionService extends IService<ChatSession> {
    // 获取或创建用户间的会话
    ChatSession getOrCreateSession(Long userId1, Long userId2);
}

