package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // 动生成所有字段的 getter/setter、toString、equals、hashCode 方法
@NoArgsConstructor // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的全参构造函数
@TableName("admin_log")
public class AdminLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String actionType;
    private String targetId;
    private String detail;
    private LocalDateTime createTime;
}