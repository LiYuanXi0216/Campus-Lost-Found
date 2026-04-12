package cn.edu.cug.campuslostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserMapper userMapper;
    // 💡 核心修改 1：在这里声明 EmailService
    private final EmailService emailService;
    private final ItemPostMapper itemPostMapper;

    // 💡 核心修改：在构造函数中同时注入这三个依赖
    public UserService(UserMapper userMapper, EmailService emailService, ItemPostMapper itemPostMapper) {
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.itemPostMapper = itemPostMapper;
    }

    // 功能 1：用户注册 (带邮箱验证码功能)
    public User register(User user) {

        // ================= 核心修改 3：第一步先拦截并校验验证码 =================
        // 确保前端传了邮箱和验证码，并且调用 emailService 验证通过
        if (user.getEmail() == null || user.getCode() == null ||
                !emailService.verifyCode(user.getEmail(), user.getCode())) {
            throw new RuntimeException("验证码错误或已过期，请重新获取！");
        }
        // ====================================================================

        // 1. 检查账号是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("该账号已被注册！");
        }

        // 💡 额外优化：检查这个邮箱是不是已经被别人注册过了
        QueryWrapper<User> emailQuery = new QueryWrapper<>();
        emailQuery.eq("email", user.getEmail());
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new RuntimeException("该邮箱已经被注册过了！");
        }

        // 2. 密码加密 (MD5) -> 即使数据库被盗，黑客也看不到原密码
        String md5Password = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(md5Password);

        // 3. 设置默认属性
        if (user.getNickname() == null) {
            user.setNickname("校园用户_" + System.currentTimeMillis() % 10000);
        }
        user.setRole("USER"); // 默认都是普通用户

        // 4. 保存到数据库
        userMapper.insert(user);

        // 为了安全，返回给前端的对象里把密码清空
        user.setPassword(null);
        return user;
    }

    // 功能 2：用户登录 (保持不变)
    public User login(String username, String password) {
        // 1. 把用户传进来的明文密码，用同样的规则加密
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2. 去数据库里同时匹配账号和加密后的密码
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        queryWrapper.eq("password", md5Password);

        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("账号或密码错误！");
        }

        // 登录成功，同样清空密码后再返回给前端
        user.setPassword(null);
        return user;
    }

    public Map<String, Object> getUserProfile(Long userId) {
        // 1. 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(null); // 安全起见清空密码

        // 💡 自动生成随机头像 (如果数据库没有)
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            user.setAvatar("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + user.getUsername());
        }

        // 2. 统计我发布的帖子数量
        QueryWrapper<ItemPost> postWrapper = new QueryWrapper<>();
        postWrapper.eq("publisher_id", userId.toString());
        long postCount = itemPostMapper.selectCount(postWrapper);

        // 3. 封装返回结果
        Map<String, Object> profile = new HashMap<>();
        profile.put("userInfo", user);

        // 预留统计模块：目前只有 postCount 是真实的
        Map<String, Object> stats = new HashMap<>();
        stats.put("postCount", postCount);
        stats.put("commentCount", 0);      // 预留未来评论数
        stats.put("subscriptionCount", 0); // 预留未来订阅数
        profile.put("stats", stats);

        return profile;
    }
}
