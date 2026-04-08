package cn.edu.cug.campuslostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils; // Spring自带的加密工具

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // 功能 1：用户注册
    public User register(User user) {
        // 1. 检查账号是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("该账号已被注册！");
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

    // 功能 2：用户登录
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
}
