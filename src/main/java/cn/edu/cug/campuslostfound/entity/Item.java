package cn.edu.cug.campuslostfound.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物品信息实体类 (寻物/招领)
 * 对应数据库中的 item 表
 */
@Data
public class Item {
    private Long id;              // 主键ID
    private Long userId;          // 发布者ID (关联user表的id)
    private Integer type;         // 信息类型: 0-丢失(寻物), 1-拾取(招领)
    private String title;         // 物品名称摘要
    private String description;   // 详细描述
    private String location;      // 丢失/拾取的大致地点
    private Integer status;       // 状态: 0-匹配中, 1-已解决
    private LocalDateTime createTime; // 发布时间
}