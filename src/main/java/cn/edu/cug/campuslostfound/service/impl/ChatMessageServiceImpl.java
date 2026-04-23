package cn.edu.cug.campuslostfound.service.impl;

import cn.edu.cug.campuslostfound.entity.ChatMessage; // 必须加
import cn.edu.cug.campuslostfound.entity.ChatSession; // 必须加
import cn.edu.cug.campuslostfound.mapper.ChatMessageMapper;
import cn.edu.cug.campuslostfound.mapper.ChatSessionMapper;
import cn.edu.cug.campuslostfound.service.ChatMessageService;
import cn.edu.cug.campuslostfound.service.ChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Override
    public ChatMessage sendMessage(Long senderId, Long receiverId, String content) {
        ChatSession session = chatSessionService.getOrCreateSession(senderId, receiverId);

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(0);
        this.save(message);

        session.setLastMsgId(message.getId());
        chatSessionMapper.updateById(session);
        return message;
    }

    @Override
    public List<ChatMessage> getMessageBySessionId(Long sessionId, Integer pageNum, Integer pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.orderByAsc("id");

        // 简单分页（如果不需要分页，直接返回 baseMapper.selectList(wrapper) 即可）
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ChatMessage> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        return baseMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public void markMessageAsRead(Long msgId) {
        ChatMessage message = new ChatMessage();
        message.setId(msgId);
        message.setIsRead(1);
        this.updateById(message);
    }
}