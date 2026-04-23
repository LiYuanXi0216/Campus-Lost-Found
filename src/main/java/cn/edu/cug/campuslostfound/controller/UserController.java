package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.service.EmailService;
import cn.edu.cug.campuslostfound.service.UserService;
import cn.edu.cug.campuslostfound.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List; // 必须加！！！
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService userService;
    @Autowired
    private EmailService emailService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 注册接口
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            User newUser = userService.register(user);
            result.put("success", true);
            result.put("data", newUser);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            User loginUser = userService.login(user.getUsername(), user.getPassword());

            // 登录成功，生成 Token 令牌！
            String token = JwtUtils.generateToken(loginUser.getId(), loginUser.getUsername(), loginUser.getRole());

            result.put("success", true);
            result.put("data", loginUser);
            result.put("token", token); // 把手环交给前端！

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // 请求发送验证码接口
    // 这里因为只需接收一个邮箱，就不用特意建个实体类了，用 Map 接收一下即可
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String email = params.get("email");
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("邮箱不能为空");
            }
            emailService.sendVerificationCode(email);
            result.put("success", true);
            result.put("message", "验证码发送成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 通过之前的拦截器拿到当前用户ID
            Long userId = (Long) request.getAttribute("currentUserId");

            // 调用聚合服务
            Map<String, Object> data = userService.getUserProfile(userId);

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ================= 新增：修改头像 API =================
    @PutMapping("/avatar")
    public Map<String, Object> updateAvatar(@RequestBody Map<String, String> params, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = (Long) request.getAttribute("currentUserId");
            String avatarUrl = params.get("avatar");
            userService.updateAvatar(userId, avatarUrl);
            result.put("success", true);
            result.put("message", "头像更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ================= 新增：获取所有用户列表（供聊天联系人用） =================
    @GetMapping("/list")
    public List<User> getAllUsers() {
        return userService.list();
    }
}