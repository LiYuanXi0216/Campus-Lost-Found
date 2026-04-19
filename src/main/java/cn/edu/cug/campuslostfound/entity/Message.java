package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 核心注解：自动生成所有字段的 getter/setter、toString、equals、hashCode 方法
@NoArgsConstructor // 自动生成无参构造函数（框架反射创建对象时通常需要）
@AllArgsConstructor // 自动生成包含所有字段的全参构造函数
@TableName("app_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type; // SUBSCRIPTION, COMMENT, PRIVATE
    private String title;
    private String content;
    private Long relatedId;
    private Boolean isRead;
    private java.time.LocalDateTime createTime;
}
