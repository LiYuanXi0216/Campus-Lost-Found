package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 核心注解：自动生成所有字段的 getter/setter、toString、equals、hashCode 方法
@NoArgsConstructor // 自动生成无参构造函数（框架反射创建对象时通常需要）
@AllArgsConstructor // 自动生成包含所有字段的全参构造函数
@TableName("app_user") // 指定对应的数据库表名
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String role; // "USER" 或 "ADMIN"
    private String email; // 用户的邮箱地址

    @TableField(exist = false) // 告诉框架：数据库里没有这个字段，它只用来接收前端传来的验证码
    private String code;

}
