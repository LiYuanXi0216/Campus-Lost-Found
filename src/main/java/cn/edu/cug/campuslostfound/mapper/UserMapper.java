package cn.edu.cug.campuslostfound.mapper;

import cn.edu.cug.campuslostfound.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表(user)的数据库操作接口
 */
@Mapper
public interface UserMapper {

    // 1. 注册：新增一个用户
    @Insert("INSERT INTO user(username, password, contact_info, create_time) " +
            "VALUES(#{username}, #{password}, #{contactInfo}, NOW())")
    // @Options 的作用是：插入成功后，自动把数据库生成的自增id赋值给传进来的user对象
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    // 2. 登录/校验：根据用户名查找用户
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
}