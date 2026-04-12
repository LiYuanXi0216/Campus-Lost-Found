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
    private LocalDateTime createTime;
    private String location;
    private String imageUrl; // 图片 URL
    private String itemStatus; // 物品状态（如：丢失、找到）
    private String incidentTime; // 丢失/捡到的描述性时间
    private String publisherId;
    private Double latitude;  // 纬度
    private Double longitude; // 经度
}
