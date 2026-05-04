package cn.edu.cug.campuslostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final EmailService emailService;
    private final ItemPostMapper itemPostMapper;
    private final AppCommentService commentService;

    public UserService(UserMapper userMapper,
                       EmailService emailService,
                       ItemPostMapper itemPostMapper,
                       AppCommentService commentService) {
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.itemPostMapper = itemPostMapper;
        this.commentService = commentService;
    }

    // 功能 1：用户注册 (带邮箱验证码功能)
    public User register(User user) {

        // 1. 先检查必填项是否为空（最先检查，避免浪费后续校验资源）
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("账号不能为空！");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空！");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空！");
        }
        if (user.getCode() == null || user.getCode().trim().isEmpty()) {
            throw new RuntimeException("验证码不能为空！");
        }

        // 2. 校验账号格式：3~20 个字符，只允许字母、数字、下划线
        String username = user.getUsername().trim();
        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("账号长度需在 3~20 个字符之间！");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("账号只能包含字母、数字和下划线！");
        }

        // 3. 校验密码长度：至少 6 位
        String password = user.getPassword().trim();
        if (password.length() < 6) {
            throw new RuntimeException("密码长度至少为 6 位！");
        }

        // 4. 校验邮箱格式
        String email = user.getEmail().trim();
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new RuntimeException("邮箱格式不正确！");
        }

        // 5. 检查账号是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("该账号已被注册！");
        }

        // 6. 检查这个邮箱是不是已经被别人注册过了
        QueryWrapper<User> emailQuery = new QueryWrapper<>();
        emailQuery.eq("email", email);
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new RuntimeException("该邮箱已经被注册过了！");
        }

        // 7. 最后校验验证码（验证码是一次性的，放最后避免浪费）
        if (!emailService.verifyCode(email, user.getCode().trim())) {
            throw new RuntimeException("验证码错误或已过期，请重新获取！");
        }

        // 8. 密码加密 (MD5)
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());
        user.setUsername(username);
        user.setPassword(md5Password);
        user.setEmail(email);

        // 9. 设置默认属性
        if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            user.setNickname("校园用户_" + System.currentTimeMillis() % 10000);
        }
        user.setRole("USER");

        // 10. 保存到数据库
        userMapper.insert(user);

        // 为了安全，返回给前端的对象里把密码清空
        user.setPassword(null);
        return user;
    }

    // 功能 2：用户登录
    // 核心修改：将 username 参数改名为 account，代表它可以是账号也可以是邮箱
    public User login(String account, String password) {

        if (account == null || account.trim().isEmpty()) {
            throw new RuntimeException("账号/邮箱不能为空！");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空！");
        }

        String md5Password = DigestUtils.md5DigestAsHex(password.trim().getBytes());

        String trimmedAccount = account.trim();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq("username", trimmedAccount)
                .or()
                .eq("email", trimmedAccount)
        );
        queryWrapper.eq("password", md5Password);

        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("账号/邮箱不存在，或密码错误！");
        }

        user.setPassword(null);
        return user;
    }

    // 功能 3：获取用户个人中心信息
    public Map<String, Object> getUserProfile(Long userId) {
        // 1. 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(null); // 安全起见清空密码

        // 2. 自动生成随机头像 (如果数据库没有)
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            user.setAvatar("https://api.dicebear.com/7.x/pixel-art/svg?seed=" + user.getUsername());
        }

        // 4. 统计我发布的帖子数量
        QueryWrapper<ItemPost> postWrapper = new QueryWrapper<>();
        postWrapper.eq("publisher_id", userId.toString());
        long postCount = itemPostMapper.selectCount(postWrapper);
        long commentCount = commentService.countCommentsByUser(userId);

        // 5. 封装返回结果
        Map<String, Object> profile = new HashMap<>();
        profile.put("userInfo", user);

        // 预留统计模块：目前只有 postCount 是真实的
        Map<String, Object> stats = new HashMap<>();
        stats.put("postCount", postCount);
        stats.put("commentCount", commentCount);
        stats.put("subscriptionCount", 0); // 预留未来订阅数
        profile.put("stats", stats);

        return profile;
    }

    // ================= 新增：修改头像 =================
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);
        }
    }

    public List<User> list() {
        // 查询数据库里所有的用户
        return userMapper.selectList(null);
    }
}
