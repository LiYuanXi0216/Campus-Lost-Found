package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import cn.edu.cug.campuslostfound.entity.PostSubscription;
import cn.edu.cug.campuslostfound.entity.CampusBuilding;
import cn.edu.cug.campuslostfound.mapper.CampusBuildingMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.util.StringUtils; // 引入Spring自带的工具类

@Service
public class ItemPostService {

    private final ItemPostMapper mapper;
    private final SubscriptionService subscriptionService;
    private final CampusBuildingMapper buildingMapper;

    public ItemPostService(ItemPostMapper mapper, SubscriptionService subscriptionService, CampusBuildingMapper buildingMapper) {
        this.mapper = mapper;
        this.subscriptionService = subscriptionService;
        this.buildingMapper = buildingMapper;
    }

    // ==========================================
    // 通用方法：清洗和补全空间数据 (抽取出的核心逻辑)
    // ==========================================
    private void cleanAndFillLocationData(ItemPost post) {
        if ("LOST".equals(post.getType())) {
            // 🐛 修复 Bug：如果是寻物启事，强行清空精确 GPS
            post.setLatitude(null);
            post.setLongitude(null);
        } else if ("FOUND".equals(post.getType())) {
            // 🚀 逻辑增强：如果是招领启事，且前端没有传来精确 GPS，但选了建筑物
            if (post.getLatitude() == null && post.getBuildingId() != null) {
                CampusBuilding building = buildingMapper.selectById(post.getBuildingId());
                if (building != null) {
                    // 继承建筑物的中心坐标作为保底
                    post.setLatitude(building.getCenterLat());
                    post.setLongitude(building.getCenterLng());
                }
            }
        }
    }

    // 业务功能 1：发布帖子
    public ItemPost createPost(ItemPost post, Long userId) {
        post.setCreateTime(java.time.LocalDateTime.now());
        post.setPublisherId(userId.toString());
        if (post.getItemStatus() == null) post.setItemStatus("PENDING");

        // 逻辑优化：如果是寻物(LOST)，强制清空经纬度，防止误导
        if ("LOST".equals(post.getType())) {
            post.setLatitude(null);
            post.setLongitude(null);
        }

        // 👉 插入前，清洗一下空间数据
        cleanAndFillLocationData(post);
        mapper.insert(post);

        try {
            subscriptionService.matchNewPostAndNotify(post);
        } catch (Exception e) {
            System.err.println("订阅通知系统异常：" + e.getMessage());
        }
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
    public List<ItemPost> searchPosts(String type, String keyword, Long buildingId, String startDate, String endDate) {
        QueryWrapper<ItemPost> wrapper = new QueryWrapper<>();

        // 1. 类型精确过滤
        if (type != null && !type.equals("ALL")) {
            wrapper.eq("type", type);
        }

        // 2. 建筑物精确过滤
        if (buildingId != null) {
            wrapper.eq("building_id", buildingId);
        }

        // 3. 时间区间交集碰撞算法
        // 只要 帖子的开始时间 <= 搜索的结束时间 且 帖子的结束时间 >= 搜索的开始时间，就说明有重叠！
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            wrapper.le("incident_start_date", endDate)
                    .ge("incident_end_date", startDate);
        }

        // 4. 伪智能分词：多关键词模糊匹配
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 将用户输入的关键词按空格切分 (例如 "黑色 校园卡" 变成 ["黑色", "校园卡"])
            String[] keywords = keyword.trim().split("\\s+");

            // 构建 AND 条件组，要求输入的每个词都必须在标题、描述或位置中出现
            wrapper.and(w -> {
                for (String kw : keywords) {
                    w.like("title", kw)
                            .or().like("description", kw)
                            .or().like("location_desc", kw)
                            .or().like("incident_time_desc", kw);
                }
            });
        }

        // 按发布时间倒序排列
        wrapper.orderByDesc("create_time");
        return mapper.selectList(wrapper);
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

        // 👉 更新进数据库前，再清洗一次（防止用户把 FOUND 改成了 LOST）
        cleanAndFillLocationData(post);

        // 5. 保存回数据库
        mapper.updateById(post);
        return post;
    }

    public List<ItemPost> getRecommendations(Long postId) {
        ItemPost myPost = mapper.selectById(postId);
        if (myPost == null) throw new RuntimeException("帖子不存在");

        // 核心逻辑 1：反转类型 (如果是寻找，就去招领里找)
        String oppositeType = myPost.getType().equals("LOST") ? "FOUND" : "LOST";

        // 核心逻辑 2：提取关键分词 (简单策略：按空格切分标题，取前两个词)
        String[] words = myPost.getTitle().split("\\s+");
        String searchKeyword = words[0]; // 这里用第一个词去模糊搜索

        // 核心逻辑 3：调用我们上一轮写好的无敌检索引擎
        // 我们放宽时间要求，只匹配类型、关键词和地点
        return searchPosts(oppositeType, searchKeyword, myPost.getBuildingId(), null, null);
    }
}
