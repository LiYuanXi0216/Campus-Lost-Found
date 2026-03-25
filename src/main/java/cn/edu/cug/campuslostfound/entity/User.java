package cn.edu.cug.campuslostfound.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库中的 user 表
 */
@Data
public class User {
    private Long id;              // 主键ID
    private String username;      // 用户名(建议用学号)
    private String password;      // 密码
    private String contactInfo;   // 联系方式(微信/手机号)
    private LocalDateTime createTime; // 注册时间
}