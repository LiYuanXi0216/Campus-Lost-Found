package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.Message;
import cn.edu.cug.campuslostfound.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // API 1: 获取我的消息列表
    @GetMapping("/my")
    public Map<String, Object> getMyMessages(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            List<Message> messages = messageService.getMyMessages(userId);
            result.put("success", true);
            result.put("data", messages);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API 2: 标记单条消息为已读
    @PutMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        messageService.markAsRead(id, userId);
        return Collections.singletonMap("success", true);
    }
}