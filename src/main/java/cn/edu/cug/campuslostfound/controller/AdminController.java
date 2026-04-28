package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

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
}