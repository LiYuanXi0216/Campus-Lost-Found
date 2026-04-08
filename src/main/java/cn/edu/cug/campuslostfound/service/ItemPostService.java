package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.util.StringUtils; // 引入Spring自带的工具类

@Service
public class ItemPostService {

    private final ItemPostMapper mapper;

    public ItemPostService(ItemPostMapper mapper) {
        this.mapper = mapper;
    }

    // 业务功能 1：发布帖子
    public ItemPost createPost(ItemPost post) {
        post.setCreateTime(LocalDateTime.now());
        if (post.getItemStatus() == null) {
            post.setItemStatus("PENDING"); // 默认状态为寻找中/招领中
        }
        mapper.insert(post);
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

    // 核心升级功能：综合检索 (支持按类型过滤 + 关键词模糊匹配)
    public List<ItemPost> searchPosts(String type, String keyword) {
        QueryWrapper<ItemPost> queryWrapper = new QueryWrapper<>();

        // 1. 如果前端传了 type (比如只看 LOST)，就加上 type 条件
        // StringUtils.hasText() 用于判断字符串是不是空的
        if (StringUtils.hasText(type) && !type.equals("ALL")) {
            queryWrapper.eq("type", type);
        }

        // 2. 如果前端传了 keyword，就在标题、描述、地点里进行模糊匹配
        if (StringUtils.hasText(keyword)) {
            // 相当于 SQL: AND (title LIKE '%keyword%' OR description LIKE '%keyword%' OR location LIKE '%keyword%')
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("description", keyword)
                    .or()
                    .like("location", keyword)
            );
        }

        // 3. 按发布时间倒序排列 (最新的帖子在最上面)
        queryWrapper.orderByDesc("create_time");

        return mapper.selectList(queryWrapper);
    }
}
