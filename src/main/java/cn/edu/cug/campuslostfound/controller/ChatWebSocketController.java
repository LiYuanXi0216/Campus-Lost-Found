package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.ChatMessage;
import cn.edu.cug.campuslostfound.service.ChatMessageService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

// ChatWebSocketController.java
@Controller
public class ChatWebSocketController {
    @Autowired
    private ChatMessageService chatMessageService;

    // 处理客户端发送的聊天消息
    @MessageMapping("/chat/send") // 客户端发送地址：/app/chat/send
    @SendToUser("/topic/chat/receive") // 推送给接收者的订阅地址
    public ChatMessage handleChatMessage(ChatMessageDTO dto) {
        // 1. 保存消息到数据库
        ChatMessage message = chatMessageService.sendMessage(
                dto.getSenderId(),
                dto.getReceiverId(),
                dto.getContent()
        );
        // 2. 推送消息给接收者
        return message;
    }

    // 辅助DTO：接收前端参数
    @Data
    public static class ChatMessageDTO {
        private Long senderId;
        private Long receiverId;
        private String content;
    }
}
