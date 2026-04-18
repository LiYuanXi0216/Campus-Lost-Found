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
        post.setCreateTime(java.time.LocalDateTime.now());
        post.setPublisherId(userId.toString());

        // 逻辑优化：如果是寻物(LOST)，强制清空经纬度，防止误导
        if ("LOST".equals(post.getType())) {
            post.setLatitude(null);
            post.setLongitude(null);
        }

        mapper.insert(post);
        return post;
    }

    // 业务功能 2：浏览所有帖子
    public List<ItemPost> getAllPosts() {
        return mapper.selectList(null); // 传 null 表示查询所有，没有条件
    }

    // 业务功能 3：根据类型（LOST 或 FOUND）分类浏览帖子
    public List<ItemPost> getPostsByType(String type) {
        // 创建一个条件构造器
        QueryWrapper<ItemPost> queryWrapper = new QueryWrapper<>();
        // 相当于 SQL 中的：WHERE type = 传入的type值
        queryWrapper.eq("type", type);

        // 让 Mapper 根据这个条件去查数据库
        return mapper.selectList(queryWrapper);
    }

    // 业务功能4：综合检索 (支持按类型过滤 + 关键词模糊匹配)
    public List<ItemPost> searchPosts(String type, String keyword) {
        QueryWrapper<ItemPost> queryWrapper = new QueryWrapper<>();

        // 分类过滤
        if (type != null && !type.equals("ALL")) {
            queryWrapper.eq("type", type);
        }

        // 关键词模糊匹配 (修改了底层的字段名)
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("description", keyword)
                    .or()
                    .like("location_desc", keyword) // 👉 以前是 location
                    .or()
                    .like("incident_time_desc", keyword) // 👉 增加对模糊时间的搜索
            );
        }
        queryWrapper.orderByDesc("create_time");
        return mapper.selectList(queryWrapper);
    }

    // 业务功能 5：查询我发布的帖子
    public List<ItemPost> getMyPosts(Long userId) {
        QueryWrapper<ItemPost> queryWrapper = new QueryWrapper<>();
        // 查询 publisher_id 等于当前登录用户 ID 的帖子
        queryWrapper.eq("publisher_id", userId.toString());
        queryWrapper.orderByDesc("create_time");
        return mapper.selectList(queryWrapper);
    }

    // 业务功能 6：删除我的帖子
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

    // 业务功能 7：修改我的帖子
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
        if (updateData.getContact() != null) post.setContact(updateData.getContact());
        if (updateData.getItemStatus() != null) post.setItemStatus(updateData.getItemStatus());
        if (updateData.getType() != null) post.setType(updateData.getType());
        if (updateData.getImageUrl() != null) post.setImageUrl(updateData.getImageUrl());
        if (updateData.getIncidentStartDate() != null) post.setIncidentStartDate(updateData.getIncidentStartDate());
        if (updateData.getIncidentEndDate() != null) post.setIncidentEndDate(updateData.getIncidentEndDate());
        if (updateData.getIncidentTimeDesc() != null) post.setIncidentTimeDesc(updateData.getIncidentTimeDesc());
        if (updateData.getBuildingId() != null) post.setBuildingId(updateData.getBuildingId());
        if (updateData.getLocationDesc() != null) post.setLocationDesc(updateData.getLocationDesc());
        if (updateData.getLatitude() != null) post.setLatitude(updateData.getLatitude());
        if (updateData.getLongitude() != null) post.setLongitude(updateData.getLongitude());


        // 5. 保存回数据库
        mapper.updateById(post);
        return post;
    }
}
