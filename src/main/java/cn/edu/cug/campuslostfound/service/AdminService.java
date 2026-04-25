package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员业务层
 * 负责处理校园失物招领系统的管理员后台核心业务逻辑
 * 包含数据统计、帖子下架、批量删除等功能
 */
@Service
public class AdminService {

    /**
     * 帖子数据访问层对象
     * 用于执行item_post表的增删改查操作
     */
    private final ItemPostMapper itemPostMapper;

    /**
     * 构造方法注入Mapper
     * Spring推荐的依赖注入方式，无冗余注解
     * @param itemPostMapper 帖子数据访问层实例
     */
    public AdminService(ItemPostMapper itemPostMapper) {
        this.itemPostMapper = itemPostMapper;
    }

    /**
     * 数据看板统计
     * 统计后台管理所需的核心数据：总帖子数、已解决帖子数、今日新增帖子数
     * 返回格式固定：success / message / totalPosts / resolvedPosts
     * @return 封装统计结果的Map集合
     */
    public Map<String, Object> getDashboardStats() {
        // 初始化返回结果集
        Map<String, Object> result = new HashMap<>();

        // 查询数据库：获取所有帖子的总数量
        long totalPosts = itemPostMapper.selectCount(null);
        // 查询数据库：获取状态为【已解决】的帖子数量
        long resolvedPosts = itemPostMapper.selectCount(
                new QueryWrapper<ItemPost>().eq("item_status", "RESOLVED")
        );

        // 计算今日开始时间（00:00:00）和今日结束时间（23:59:59.999999999）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        // 查询数据库：获取今日创建的帖子数量
        long todayPosts = itemPostMapper.selectCount(
                new QueryWrapper<ItemPost>()
                        .between("create_time", todayStart, todayEnd)
        );

        // 封装返回结果：操作成功标识
        result.put("success", true);
        // 封装返回结果：提示信息（包含今日新增帖子数）
        result.put("message", "管理员看板统计获取成功，今日新增帖子 " + todayPosts + " 条");
        // 封装返回结果：总帖子数
        result.put("totalPosts", totalPosts);
        // 封装返回结果：已解决帖子数
        result.put("resolvedPosts", resolvedPosts);
        return result;
    }

    /**
     * 强制下架帖子
     * 业务逻辑：将帖子状态修改为BLOCKED，实现下架效果
     * @param id 帖子主键ID
     * @return 封装操作结果的Map集合
     */
    public Map<String, Object> blockPost(Long id) {
        // 初始化返回结果集
        Map<String, Object> result = new HashMap<>();

        // 根据ID查询帖子信息
        ItemPost post = itemPostMapper.selectById(id);
        // 校验：帖子不存在，返回失败结果
        if (post == null) {
            result.put("success", false);
            result.put("message", "帖子不存在");
            return result;
        }

        // 修改帖子状态为【已下架】
        post.setItemStatus("BLOCKED");
        // 根据ID更新数据库中的帖子信息
        itemPostMapper.updateById(post);

        // 封装返回结果：操作成功
        result.put("success", true);
        result.put("message", "帖子 " + id + " 已被强制下架");
        return result;
    }

    /**
     * 批量删除帖子
     * 接收前端传递的ID集合，循环执行删除操作
     * 请求体参数格式：{ "ids": [1,2,3] }
     * @param params 前端传递的参数集合
     * @return 封装删除结果的Map集合
     */
    public Map<String, Object> batchDeletePosts(Map<String, Object> params) {
        // 初始化返回结果集
        Map<String, Object> result = new HashMap<>();

        // 从参数中获取ID列表对象
        Object idsObj = params.get("ids");
        // 校验：参数不是List集合 或者 集合为空，返回错误提示
        if (!(idsObj instanceof List<?> ids) || ids.isEmpty()) {
            result.put("success", false);
            result.put("message", "请传入要删除的帖子ID列表");
            return result;
        }

        // 统计成功删除的帖子数量
        int deletedCount = 0;
        // 循环遍历ID列表，逐个删除帖子
        for (Object idObj : ids) {
            // 跳过空的ID值
            if (idObj == null) {
                continue;
            }
            // 将对象转换为Long类型的ID
            Long id = Long.valueOf(idObj.toString());
            // 执行删除操作，累加删除成功的数量
            deletedCount += itemPostMapper.deleteById(id);
        }

        // 封装返回结果：批量删除成功，展示删除总数
        result.put("success", true);
        result.put("message", "批量删除执行成功，共删除 " + deletedCount + " 条帖子");
        return result;
    }
}