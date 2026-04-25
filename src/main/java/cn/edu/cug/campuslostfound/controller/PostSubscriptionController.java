package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.PostSubscription;
import cn.edu.cug.campuslostfound.mapper.PostSubscriptionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin
public class PostSubscriptionController {
    @Autowired
    private PostSubscriptionMapper mapper;

    // 获取我的所有订阅
    @GetMapping("/my")
    public List<PostSubscription> getMySubs(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return mapper.selectList(new QueryWrapper<PostSubscription>().eq("user_id", userId));
    }

    // 取消订阅
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteSub(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        PostSubscription sub = mapper.selectById(id);
        if (sub != null && sub.getUserId().equals(userId)) {
            mapper.deleteById(id);
        }
        return Collections.singletonMap("success", true);




    }
}
