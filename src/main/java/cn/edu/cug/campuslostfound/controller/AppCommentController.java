package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.AppComment;
import cn.edu.cug.campuslostfound.service.AppCommentService;
import cn.edu.cug.campuslostfound.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin
// 评论模块控制器：
// 1. 提供帖子评论树查询接口
// 2. 提供新增评论 / 回复接口
// 3. 提供点赞 / 取消点赞接口
//
// 这里故意保持“轻控制器”风格：
// - 参数接收、登录态解析放在控制器层
// - 业务校验、树结构组装、通知推送放在 Service 层
public class AppCommentController {

    private final AppCommentService commentService;

    public AppCommentController(AppCommentService commentService) {
        this.commentService = commentService;
    }

    // API 1：获取某条帖子下的评论树（公开可见，不要求登录）
    // 前端详情弹窗打开时调用这个接口。
    // 即使用户没有登录，也允许查看评论；只是这时拿不到“我是否点过赞”的个性化状态。
    @GetMapping("/post/{postId}")
    public Map<String, Object> getPostComments(@PathVariable Long postId,
                                               @RequestParam(defaultValue = "latest") String sortBy,
                                               HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AppComment> comments = commentService.getPostComments(postId, sortBy, resolveOptionalUserId(request));
            result.put("success", true);
            result.put("data", comments);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API 2：新增评论或回复（是否是回复由 parentCommentId 是否为空决定）
    // 这里前端统一提交 AppComment 结构：
    // - 顶级评论：parentCommentId = null
    // - 回复：parentCommentId = 被回复的评论 ID
    //
    // 真正的“这是不是回复”“它属于哪条评论串”这些衍生字段，
    // 统一在 Service 层重新计算，避免前端伪造或传错。
    @PostMapping
    public Map<String, Object> createComment(@RequestBody AppComment comment, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            AppComment createdComment = commentService.createComment(comment, userId);
            result.put("success", true);
            result.put("data", createdComment);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API 3：点赞评论或回复
    // 采用“切换式”接口：
    // - 如果当前用户还没点过赞，就新增点赞记录并 like_count + 1
    // - 如果已经点过赞，就删除点赞记录并 like_count - 1
    //
    // 这样前端只需要调用同一个接口，不需要区分点赞/取消点赞两个动作。
    @PostMapping("/{id}/like")
    public Map<String, Object> likeComment(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            AppComment updatedComment = commentService.likeComment(id, userId);
            result.put("success", true);
            result.put("data", updatedComment);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 公开查询评论时，请求可能没有经过需要登录的拦截逻辑。
    // 因此这里做一层“尽力而为”的用户解析：
    // - 如果 request 里已经被上游放入 currentUserId，就直接拿
    // - 否则尝试从 Authorization 里自己解析 token
    // - 再不行就返回 null，当作游客请求处理
    private Long resolveOptionalUserId(HttpServletRequest request) {
        Object currentUserId = request.getAttribute("currentUserId");
        if (currentUserId instanceof Long userId) {
            return userId;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            Claims claims = JwtUtils.parseToken(token);
            return Long.valueOf(claims.get("userId").toString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
