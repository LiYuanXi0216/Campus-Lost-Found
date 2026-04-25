package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.ChatMessage;
import cn.edu.cug.campuslostfound.entity.ChatSession;
import cn.edu.cug.campuslostfound.service.ChatMessageService;
import cn.edu.cug.campuslostfound.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatSessionService chatSessionService;

    @GetMapping("/messages")
    public List<ChatMessage> getMessageBySessionId(
            @RequestParam Long sessionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return chatMessageService.getMessageBySessionId(sessionId, pageNum, pageSize);
    }

    @PostMapping("/read/{msgId}")
    public void markMessageRead(@PathVariable Long msgId) {
        chatMessageService.markMessageAsRead(msgId);
    }

    @GetMapping("/session")
    public ChatSession getOrCreateSession(@RequestParam Long userId1, @RequestParam Long userId2) {
        return chatSessionService.getOrCreateSession(userId1, userId2);
    }

    // 👇 新增：REST API 发送消息（直接写入数据库）
    @PostMapping("/send")
    public ChatMessage sendMessage(@RequestBody java.util.Map<String, Object> params) {
        Long senderId = Long.valueOf(params.get("senderId").toString());
        Long receiverId = Long.valueOf(params.get("receiverId").toString());
        String content = params.get("content").toString();
        return chatMessageService.sendMessage(senderId, receiverId, content);
    }
}