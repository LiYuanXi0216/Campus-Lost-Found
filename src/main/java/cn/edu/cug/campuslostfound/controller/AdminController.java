package cn.edu.cug.campuslostfound.controller;

// 👇 1. 确保导入了这两个类
import cn.edu.cug.campuslostfound.entity.ChatMessage;
import cn.edu.cug.campuslostfound.service.AdminService;
import cn.edu.cug.campuslostfound.service.ChatMessageService; // 必须有
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    // 👇 2. 确保字段是 final 的
    private final AdminService adminService;
    private final ChatMessageService chatMessageService;

    // 👇 3. 确保构造器里包含了这两个参数
    public AdminController(AdminService adminService, ChatMessageService chatMessageService) {
        this.adminService = adminService;
        this.chatMessageService = chatMessageService;
    }

    // ... 下面的接口代码保持不变 ...
    // 1. 获取大盘统计
    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> stats = adminService.getDashboardStats();
            result.put("success", true);
            result.put("data", stats);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 2. 强制删除单条违规帖子
    @DeleteMapping("/posts/{id}")
    public Map<String, Object> deletePost(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long adminId = (Long) request.getAttribute("currentUserId");
            adminService.deletePostAsAdmin(id, adminId);
            result.put("success", true);
            result.put("message", "违规帖子删除成功，操作已记录入档。");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 3. 批量删除帖子
    @DeleteMapping("/posts/batch")
    public Map<String, Object> batchDeletePosts(@RequestBody Map<String, List<Long>> params, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long adminId = (Long) request.getAttribute("currentUserId");
            List<Long> ids = params.get("ids");
            adminService.batchDeletePostsAsAdmin(ids, adminId);
            result.put("success", true);
            result.put("message", "批量删除执行成功，操作已记录入档。");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 4. 获取审计日志
    @GetMapping("/logs")
    public Map<String, Object> getLogs() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("data", adminService.getRecentLogs());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 5. 管理员获取所有私信
    @GetMapping("/messages")
    public Map<String, Object> getAllMessages() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ChatMessage> messages = chatMessageService.getAllMessages();
            result.put("success", true);
            result.put("data", messages);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 6. 管理员删除私信
    @DeleteMapping("/messages/{id}")
    public Map<String, Object> deleteMessage(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long adminId = (Long) request.getAttribute("currentUserId");
            adminService.deleteMessageAsAdmin(id, adminId);
            result.put("success", true);
            result.put("message", "违规私信删除成功，已通知相关用户，操作已记录入档。");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}