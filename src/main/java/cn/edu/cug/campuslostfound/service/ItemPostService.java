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
    public ItemPost createPost(ItemPost post, Long userId) {
        post.setCreateTime(LocalDateTime.now());
        post.setPublisherId(userId.toString()); // 自动识别并绑定发帖人！
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

    // 新增功能 1：查询我发布的帖子
    public List<ItemPost> getMyPosts(Long userId) {
        QueryWrapper<ItemPost> queryWrapper = new QueryWrapper<>();
        // 查询 publisher_id 等于当前登录用户 ID 的帖子
        queryWrapper.eq("publisher_id", userId.toString());
        queryWrapper.orderByDesc("create_time");
        return mapper.selectList(queryWrapper);
    }

    // 新增功能 2：删除我的帖子
    public void deleteMyPost(Long postId, Long userId) {
        // 1. 先查出这个帖子是否存在
        ItemPost post = mapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在！");
        }

        // 2. 【核心安全校验】：判断帖子的主人是不是当前用户
        if (!post.getPublisherId().equals(userId.toString())) {
            throw new RuntimeException("非法操作：您无权删除别人的帖子！");
        }

        // 3. 校验通过，执行删除
        mapper.deleteById(postId);
    }

    // 新增功能 3：修改我的帖子
    public ItemPost updateMyPost(Long postId, ItemPost updateData, Long userId) {
        ItemPost post = mapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在！");
        }

        if (!post.getPublisherId().equals(userId.toString())) {
            throw new RuntimeException("非法操作：您无权修改别人的帖子！");
        }

        // 4. 将前端传来的新数据覆盖旧数据 (这里只允许修改部分字段)
        if (updateData.getTitle() != null) post.setTitle(updateData.getTitle());
        if (updateData.getDescription() != null) post.setDescription(updateData.getDescription());
        if (updateData.getLocation() != null) post.setLocation(updateData.getLocation());
        if (updateData.getContact() != null) post.setContact(updateData.getContact());
        if (updateData.getItemStatus() != null) post.setItemStatus(updateData.getItemStatus());
        if (updateData.getType() != null) post.setType(updateData.getType());

        // 5. 保存回数据库
        mapper.updateById(post);
        return post;
    }
}
