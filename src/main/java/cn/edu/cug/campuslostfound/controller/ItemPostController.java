package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.entity.PostSubscription;
import cn.edu.cug.campuslostfound.service.ItemPostService;
import cn.edu.cug.campuslostfound.service.SubscriptionService;
import cn.edu.cug.campuslostfound.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // 标记这是一个返回 JSON 数据的接口类
@RequestMapping("/api/posts") // 规定这些接口的统一前缀路径
@CrossOrigin // 简单粗暴地允许前端跨域请求，方便你们前后端本地联调
public class ItemPostController {

    private final ItemPostService service;
    private final SubscriptionService subscriptionService;

    public ItemPostController(ItemPostService service, SubscriptionService subscriptionService) {
        this.service = service;
        this.subscriptionService = subscriptionService;
    }

    // API 1: 发布帖子 (使用 POST 请求)
    @PostMapping
    public Map<String, Object> create(@RequestBody ItemPost post, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            ItemPost newPost = service.createPost(post, userId);
            result.put("success", true);
            result.put("data", newPost);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API 2: 浏览所有帖子 (使用 GET 请求)
    @GetMapping
    public List<ItemPost> listAll() {
        return service.getAllPosts();
    }

    // API 3: 根据类型浏览帖子
    // 路径会长这样：http://localhost:8080/api/posts/type/LOST
    @GetMapping("/type/{type}")
    public List<ItemPost> listByType(@PathVariable String type) {
        // @PathVariable 会把路径里的 {type} 提取出来传给方法的参数
        return service.getPostsByType(type);
    }

    // API 4: 综合检索接口
    // 访问路径类似：http://localhost:8080/api/posts/search?type=LOST&keyword=黑色
    @GetMapping("/search")
    public List<ItemPost> search(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) { // 👈 新增 request 参数

        List<ItemPost> posts = service.searchPosts(type, keyword, buildingId, startDate, endDate);

        // ==========================================
        // 🚨 核心修复：手动解析 Token 识别当前用户
        // ==========================================
        Long userId = null;
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            try {
                // 解开令牌，拿到真实用户ID
                userId = Long.valueOf(JwtUtils.parseToken(token).get("userId").toString());
            } catch (Exception e) {
                // Token 过期或无效，按游客处理，不报错
            }
        }

        // 将真实用户ID传给脱敏中心
        service.sanitizePostList(posts, userId);

        return posts;
    }

    // API 5: 获取我发布的帖子 (GET 请求)
    @GetMapping("/my")
    public List<ItemPost> getMyPosts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return service.getMyPosts(userId);
    }

    // API 6: 修改我的帖子 (PUT 请求)
    // 路径例如：PUT /api/posts/{id}
    @PutMapping("/{id}")
    public Map<String, Object> updateMyPost(@PathVariable Long id, @RequestBody ItemPost post, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            ItemPost updatedPost = service.updateMyPost(id, post, userId);
            result.put("success", true);
            result.put("data", updatedPost);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API 7: 删除我的帖子 (DELETE 请求)
    // 路径例如：DELETE /api/posts/{id}
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteMyPost(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            service.deleteMyPost(id, userId);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API: 主动获取相似匹配帖子
    @GetMapping("/{id}/recommendations")
    public Map<String, Object> getRecommendations(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ItemPost> matches = service.getRecommendations(id);
            result.put("success", true);
            result.put("data", matches);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // API: 开启订阅
    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody PostSubscription sub, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            subscriptionService.subscribe(userId, sub.getKeyword(), sub.getBuildingId());
            result.put("success", true);
            result.put("message", "订阅成功！有新匹配将邮件通知您。");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }


    // 🚀 新增：验证答案接口
    @PostMapping("/{id}/verify")
    public Map<String, Object> verifyContact(@PathVariable Long id, @RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String answer = params.get("answer");
            String realContact = service.verifyAnswer(id, answer);
            result.put("success", true);
            result.put("data", realContact);
            result.put("message", "验证成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
