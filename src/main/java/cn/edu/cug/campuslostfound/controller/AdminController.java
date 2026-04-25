package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> getDashboardStats() {
        return adminService.getDashboardStats();
    }

    @PutMapping("/posts/{id}/block")
    public Map<String, Object> blockPost(@PathVariable Long id) {
        return adminService.blockPost(id);
    }

    @DeleteMapping("/posts/batch")
    public Map<String, Object> batchDeletePosts(@RequestBody Map<String, Object> params) {
        return adminService.batchDeletePosts(params);
    }
}