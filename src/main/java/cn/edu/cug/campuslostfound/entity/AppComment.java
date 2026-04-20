package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("app_comment") // 评论和回复统一对应 app_comment 表
public class AppComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publisherId; // 发布者用户 ID
    private Long postId; // 评论所属帖子 ID
    private Boolean isReply; // 是否为回复
    private Long parentCommentId; // 直接回复的评论 ID
    private Long rootCommentId; // 所属顶级评论 ID
    private String content; // 文字内容
    private String imageUrl; // 评论附带图片
    private LocalDateTime createTime; // 发表评论时间
    private Integer likeCount; // 点赞数

    @TableField(exist = false)
    private String publisherName; // 前端展示用：发布者昵称

    @TableField(exist = false)
    private String publisherAvatar; // 前端展示用：发布者头像

    @TableField(exist = false)
    private String replyToUserName; // 如果是回复，展示“回复 xxx”

    @TableField(exist = false)
    private List<AppComment> replies = new ArrayList<>(); // 前端直接消费的树形回复列表
}
