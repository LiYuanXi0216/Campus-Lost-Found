package cn.edu.cug.campuslostfound.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin") // 核心：所有接口必须以 /api/admin 开头
@CrossOrigin
public class AdminController {

    // 预留功能 1：数据看板统计 (例如：总帖子数、今日新增、已解决数量)
    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "我是管理员看板，你成功进来了！");
        // 未来在这里调用 Service 查数据库聚合数据
        result.put("totalPosts", 158);
        result.put("resolvedPosts", 42);
        return result;
    }

    // 预留功能 2：帖子管控 (强制下架违规帖子)
    @PutMapping("/posts/{id}/block")
    public Map<String, Object> blockPost(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "帖子 " + id + " 已被强制下架");
        return result;
    }

    // 预留功能 3：批量删除帖子
    @DeleteMapping("/posts/batch")
    public Map<String, Object> batchDeletePosts(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "批量删除执行成功");
        return result;
    }
}
