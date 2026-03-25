package cn.edu.cug.campuslostfound.mapper;

import cn.edu.cug.campuslostfound.entity.Item;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 物品信息表(item)的数据库操作接口
 */
@Mapper
public interface ItemMapper {

    // 1. 发布：新增一条寻物/招领信息 (status默认为0-匹配中)
    @Insert("INSERT INTO item(user_id, type, title, description, location, status, create_time) " +
            "VALUES(#{userId}, #{type}, #{title}, #{description}, #{location}, 0, NOW())")
    int insert(Item item);

    // 2. 搜索：根据信息类型和关键字模糊查询，并按时间倒序排列 (最新的在最前面)
    @Select("SELECT * FROM item WHERE type = #{type} AND title LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<Item> searchByTypeAndKeyword(Integer type, String keyword);
}