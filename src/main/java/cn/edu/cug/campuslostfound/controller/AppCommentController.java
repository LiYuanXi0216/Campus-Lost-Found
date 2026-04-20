package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.AppComment;
import cn.edu.cug.campuslostfound.service.AppCommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin
public class AppCommentController {

    private final AppCommentService commentService;

    public AppCommentController(AppCommentService commentService) {
        this.commentService = commentService;
    }

    // API 1：获取某条帖子下的评论树（公开可见，不要求登录）
    @GetMapping("/post/{postId}")
    public Map<String, Object> getPostComments(@PathVariable Long postId,
                                               @RequestParam(defaultValue = "latest") String sortBy) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AppComment> comments = commentService.getPostComments(postId, sortBy);
            result.put("success", true);
            result.put("data", comments);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API 2：新增评论或回复（是否是回复由 parentCommentId 是否为空决定）
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
    @PostMapping("/{id}/like")
    public Map<String, Object> likeComment(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            AppComment updatedComment = commentService.likeComment(id);
            result.put("success", true);
            result.put("data", updatedComment);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
