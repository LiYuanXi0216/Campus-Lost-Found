package cn.edu.cug.campuslostfound.service.impl;

import cn.edu.cug.campuslostfound.entity.ChatMessage;
import cn.edu.cug.campuslostfound.entity.ChatSession;
import cn.edu.cug.campuslostfound.entity.User; // 新增：引入User实体
import cn.edu.cug.campuslostfound.mapper.ChatMessageMapper;
import cn.edu.cug.campuslostfound.mapper.ChatSessionMapper;
import cn.edu.cug.campuslostfound.mapper.UserMapper; // 新增：引入UserMapper
import cn.edu.cug.campuslostfound.service.ChatMessageService;
import cn.edu.cug.campuslostfound.service.ChatSessionService;
import cn.edu.cug.campuslostfound.service.MessageService; // 新增：引入MessageService
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

    // 👇 新增：注入需要的 Service/Mapper
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;

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

        // 👇 新增：发送私信通知（核心逻辑）
        sendPrivateMessageNotice(senderId, receiverId);

        return message;
    }

    // 👇 新增：独立的通知发送方法（代码更清晰）
    private void sendPrivateMessageNotice(Long senderId, Long receiverId) {
        // 1. 避免自己给自己发消息也通知
        if (senderId.equals(receiverId)) {
            return;
        }

        // 2. 查询发送者的昵称
        String senderNickname = "用户" + senderId; // 默认值
        try {
            User sender = userMapper.selectById(senderId);
            if (sender != null && sender.getNickname() != null && !sender.getNickname().trim().isEmpty()) {
                senderNickname = sender.getNickname();
            }
        } catch (Exception e) {
            // 查不到昵称也没关系，用默认值，不影响主流程
        }

        // 3. 调用你现有的 pushMessage（和评论通知完全一致的风格）
        messageService.pushMessage(
                receiverId,                              // 接收通知的人
                "PRIVATE_MESSAGE",                       // 消息类型
                "您收到一条新私信",                        // 标题
                "来自用户：" + senderNickname,            // 内容
                senderId                                 // 关联ID = 发送者ID（方便前端跳转聊天）
        );
    }

    @Override
    public List<ChatMessage> getMessageBySessionId(Long sessionId, Integer pageNum, Integer pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.orderByAsc("id");

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