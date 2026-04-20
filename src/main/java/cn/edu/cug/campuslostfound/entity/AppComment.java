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
// 评论实体：
// 1. 持久化字段映射数据库里的 app_comment 表
// 2. 非持久化字段用于前端直接展示，避免前端自己再拼昵称、头像、回复关系
public class AppComment {

    @TableId(type = IdType.AUTO)
    private Long id; // 评论主键

    private Long publisherId; // 发布者用户 ID
    private Long postId; // 这条评论所属的帖子 ID
    private Boolean isReply; // 是否为回复：false=顶级评论，true=回复
    private Long parentCommentId; // 直接回复的目标评论 ID
    private Long rootCommentId; // 所属评论串的顶级评论 ID，用于后端组装树
    private String content; // 评论文字内容；允许为空，但不能和 imageUrl 同时为空
    private String imageUrl; // 评论附带的图片 URL（当前约定最多一张）
    private LocalDateTime createTime; // 评论创建时间
    private Integer likeCount; // 当前累计点赞数

    @TableField(exist = false)
    private String publisherName; // 前端展示用：发布者昵称 / 用户名

    @TableField(exist = false)
    private String publisherAvatar; // 前端展示用：发布者头像 URL

    @TableField(exist = false)
    private String replyToUserName; // 如果是回复，前端展示“回复 xxx”时所需的用户名

    @TableField(exist = false)
    private Boolean likedByCurrentUser; // 当前登录用户是否已经对这条评论点过赞

    @TableField(exist = false)
    private List<AppComment> replies = new ArrayList<>(); // 前端直接消费的回复列表（挂在顶级评论下）
}
