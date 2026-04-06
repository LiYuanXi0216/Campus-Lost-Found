package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Service
public class ItemPostService {

    private final ItemPostMapper mapper;

    public ItemPostService(ItemPostMapper mapper) {
        this.mapper = mapper;
    }

    // 业务功能 1：发布帖子
    public ItemPost createPost(ItemPost post) {
        post.setCreateTime(LocalDateTime.now());
        mapper.insert(post); // MyBatis-Plus 自带的插入方法
        return post;
    }

    // 业务功能 2：浏览所有帖子
    public List<ItemPost> getAllPosts() {
        return mapper.selectList(null); // 传 null 表示查询所有，没有条件
    }

    // 新增业务功能 3：根据类型（LOST 或 FOUND）分类浏览帖子
    public List<ItemPost> getPostsByType(String type) {
        // 创建一个条件构造器
        QueryWrapper<ItemPost> queryWrapper = new QueryWrapper<>();
        // 相当于 SQL 中的：WHERE type = 传入的type值
        queryWrapper.eq("type", type);

        // 让 Mapper 根据这个条件去查数据库
        return mapper.selectList(queryWrapper);
    }
}
