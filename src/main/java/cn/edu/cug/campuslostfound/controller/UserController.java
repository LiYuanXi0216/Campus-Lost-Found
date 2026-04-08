package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.service.UserService;
import cn.edu.cug.campuslostfound.utils.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserService userService;

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

            // ================= 新增代码 =================
            // 登录成功，生成 Token 令牌！
            String token = JwtUtils.generateToken(loginUser.getId(), loginUser.getUsername());

            result.put("success", true);
            result.put("data", loginUser);
            result.put("token", token); // 把手环交给前端！
            // ==========================================

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
