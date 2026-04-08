package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("item_post") // 对应数据库里的 item_post 表
public class ItemPost {
    @TableId(type = IdType.AUTO) // 告诉框架 id 是主键，并且是数据库自增的
    private Long id;

    private String type;
    private String title;
    private String description;
    private String contact;
    private LocalDateTime createTime;
    private String location; // 发生地点
    private String imageUrl; // 图片链接 (先预留这个字段)
    private String itemStatus; // 物品状态，默认为 "PENDING" (处理中)
    // 1. 预留给“模糊填写”：丢失/捡到的描述性时间 (如："昨天下午", "上周二")
    private String incidentTime;
    // 2. 预留给“权限控制”：是谁发布了这条帖子？（未来关联 User 表的 id）
    private String publisherId;
    // 3. 预留给“快捷上传/地图展示”：精确的经纬度定位
    private Double latitude;  // 纬度
    private Double longitude; // 经度

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getIncidentTime() { return incidentTime; }
    public void setIncidentTime(String incidentTime) { this.incidentTime = incidentTime;}

    public String getPublisherId() { return publisherId; }
    public void setPublisherId(String publisherId) { this.publisherId = publisherId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
