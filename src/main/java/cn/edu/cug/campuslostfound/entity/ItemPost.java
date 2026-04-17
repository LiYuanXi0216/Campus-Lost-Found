package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // 动生成所有字段的 getter/setter、toString、equals、hashCode 方法
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的全参构造函数
@TableName("item_post") // 对应数据库里的 item_post 表
public class ItemPost {
    @TableId(type = IdType.AUTO) // 告诉框架 id 是主键，并且是数据库自增的
    private Long id;

    private String type;
    private String title;
    private String description;
    private String contact;
    private String imageUrl; // 图片 URL
    private String itemStatus; // 物品状态（如：丢失、找到）
    // 1. 重构的时间字段
    private java.time.LocalDate incidentStartDate; // 起始日期
    private java.time.LocalDate incidentEndDate;   // 结束日期
    private String incidentTimeDesc;               // 模糊时间描述

    // 2. 重构的地点字段
    private Long buildingId;       // 绑定的建筑字典ID
    private String locationDesc;   // 详细位置说明

    // GPS 经纬度 (保持不变，但业务上变为非必填)
    private Double latitude;
    private Double longitude;

    private java.time.LocalDateTime createTime;
    private String publisherId;
}
