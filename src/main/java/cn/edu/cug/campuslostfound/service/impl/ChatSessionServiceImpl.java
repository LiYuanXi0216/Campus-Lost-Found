package cn.edu.cug.campuslostfound.service.impl;

import cn.edu.cug.campuslostfound.entity.ChatSession; // 必须加
import cn.edu.cug.campuslostfound.mapper.ChatSessionMapper;
import cn.edu.cug.campuslostfound.service.ChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    @Override
    public ChatSession getOrCreateSession(Long userId1, Long userId2) {
        Long u1 = Math.min(userId1, userId2);
        Long u2 = Math.max(userId1, userId2);

        ChatSession session = baseMapper.selectByUserPair(u1, u2);
        if (session == null) {
            session = new ChatSession();
            session.setUser1Id(u1);
            session.setUser2Id(u2);
            this.save(session);
        }
        return session;
    }
}