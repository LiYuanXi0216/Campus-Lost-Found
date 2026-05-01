package cn.edu.cug.campuslostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.cug.campuslostfound.entity.AdminLog;
import cn.edu.cug.campuslostfound.entity.ChatMessage;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.AdminLogMapper;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor // Lombok 自动生成包含所有 final 字段的构造器
public class AdminService {

    // ==============================
    // 注入的依赖（全部 final，职责分明）
    // ==============================
    private final ItemPostMapper postMapper;
    private final UserMapper userMapper;
    private final AdminLogMapper adminLogMapper;
    private final ChatMessageService chatMessageService; // 只管私信 CRUD
    private final MessageService messageService; // 只管发系统通知

    // ==========================================
    // 功能 1：记录审计日志（内部私有方法，复用）
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
    // 功能 2：获取看板大盘统计数据（原有）
    // ==========================================
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userMapper.selectCount(null);
        long totalPosts = postMapper.selectCount(null);
        long resolvedPosts = postMapper.selectCount(new QueryWrapper<ItemPost>().eq("item_status", "RESOLVED"));
        long pendingPosts = totalPosts - resolvedPosts;

        stats.put("totalUsers", totalUsers);
        stats.put("totalPosts", totalPosts);
        stats.put("resolvedPosts", resolvedPosts);
        stats.put("pendingPosts", pendingPosts);
        return stats;
    }

    // ==========================================
    // 功能 3：管理员强制删除他人帖子（原有）
    // ==========================================
    @Transactional
    public void deletePostAsAdmin(Long postId, Long adminId) {
        ItemPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在！");
        }

        postMapper.deleteById(postId);

        String detail = String.format("管理员删除了违规帖子。原帖标题: [%s], 原发布者ID: [%s]", post.getTitle(), post.getPublisherId());
        logAction(adminId, "DELETE_POST", postId.toString(), detail);
    }

    // ==========================================
    // 功能 4：管理员批量删除帖子（原有）
    // ==========================================
    @Transactional
    public void batchDeletePostsAsAdmin(List<Long> postIds, Long adminId) {
        if (postIds == null || postIds.isEmpty()) {
            throw new RuntimeException("未选中任何帖子！");
        }

        postMapper.deleteBatchIds(postIds);

        String detail = String.format("管理员执行了批量删除操作，共删除了 %d 条记录。", postIds.size());
        logAction(adminId, "BATCH_DELETE", postIds.toString(), detail);
    }

    // ==========================================
    // 功能 5：获取最近的审计日志（原有）
    // ==========================================
    public List<AdminLog> getRecentLogs() {
        QueryWrapper<AdminLog> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time").last("LIMIT 50");
        return adminLogMapper.selectList(wrapper);
    }

    // ==========================================
    // 功能 6：管理员删除私信（新增，核心逻辑）
    // ==========================================
    @Transactional
    public void deleteMessageAsAdmin(Long msgId, Long adminId) {
        // 1. 先查私信是否存在（用 ChatMessageService）
        ChatMessage msg = chatMessageService.getById(msgId);
        if (msg == null) {
            throw new RuntimeException("私信不存在！");
        }

        // 2. 删除私信（用 ChatMessageService）
        chatMessageService.removeById(msgId);

        // 3. 发系统通知给发送者和接收者（用 MessageService，复用你原有的 pushMessage）
        messageService.pushMessage(
                msg.getSenderId(),
                "SYSTEM",
                "私信删除通知",
                "您发送的一条私信已被管理员删除。",
                msgId
        );
        messageService.pushMessage(
                msg.getReceiverId(),
                "SYSTEM",
                "私信删除通知",
                "您收到的一条私信已被管理员删除。",
                msgId
        );

        // 4. 记录审计日志（复用 logAction）
        String detail = String.format("管理员删除了违规私信。私信ID: [%s], 发送者ID: [%s], 接收者ID: [%s], 内容: [%s]",
                msgId, msg.getSenderId(), msg.getReceiverId(), msg.getContent());
        logAction(adminId, "DELETE_MESSAGE", msgId.toString(), detail);
    }
}