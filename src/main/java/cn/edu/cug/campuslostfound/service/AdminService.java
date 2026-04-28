package cn.edu.cug.campuslostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.cug.campuslostfound.entity.AdminLog;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.AdminLogMapper;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final ItemPostMapper postMapper;
    private final UserMapper userMapper;
    private final AdminLogMapper adminLogMapper;

    public AdminService(ItemPostMapper postMapper, UserMapper userMapper, AdminLogMapper adminLogMapper) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.adminLogMapper = adminLogMapper;
    }

    // ==========================================
    // 功能 1：记录审计日志 (内部私有方法)
    // ==========================================
    private void logAction(Long adminId, String actionType, String targetId, String detail) {
        AdminLog log = new AdminLog();
        log.setAdminId(adminId);
        log.setActionType(actionType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        adminLogMapper.insert(log);
    }

    // ==========================================
    // 功能 2：获取看板大盘统计数据
    // ==========================================
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 统计总用户数
        long totalUsers = userMapper.selectCount(null);
        // 2. 统计总帖子数
        long totalPosts = postMapper.selectCount(null);
        // 3. 统计已解决的帖子数
        long resolvedPosts = postMapper.selectCount(new QueryWrapper<ItemPost>().eq("item_status", "RESOLVED"));
        // 4. 统计待解决的帖子数
        long pendingPosts = totalPosts - resolvedPosts;

        stats.put("totalUsers", totalUsers);
        stats.put("totalPosts", totalPosts);
        stats.put("resolvedPosts", resolvedPosts);
        stats.put("pendingPosts", pendingPosts);
        return stats;
    }

    // ==========================================
    // 功能 3：管理员强制删除他人帖子 (带事务)
    // ==========================================
    @Transactional // 保证删除帖子和写日志同时成功或同时失败
    public void deletePostAsAdmin(Long postId, Long adminId) {
        ItemPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在！");
        }

        // 1. 执行删除
        postMapper.deleteById(postId);

        // 2. 记录审计日志
        String detail = String.format("管理员删除了违规帖子。原帖标题: [%s], 原发布者ID: [%s]", post.getTitle(), post.getPublisherId());
        logAction(adminId, "DELETE_POST", postId.toString(), detail);
    }

    // ==========================================
    // 功能 4：管理员批量删除帖子 (带事务)
    // ==========================================
    @Transactional
    public void batchDeletePostsAsAdmin(List<Long> postIds, Long adminId) {
        if (postIds == null || postIds.isEmpty()) {
            throw new RuntimeException("未选中任何帖子！");
        }

        // 1. 批量删除
        postMapper.deleteBatchIds(postIds);

        // 2. 记录审计日志
        String detail = String.format("管理员执行了批量删除操作，共删除了 %d 条记录。", postIds.size());
        logAction(adminId, "BATCH_DELETE", postIds.toString(), detail);
    }

    // ==========================================
    // 功能 5：获取最近的审计日志
    // ==========================================
    public List<AdminLog> getRecentLogs() {
        // 只查询最近的 50 条操作记录，防止数据量过大
        QueryWrapper<AdminLog> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time").last("LIMIT 50");
        return adminLogMapper.selectList(wrapper);
    }
}