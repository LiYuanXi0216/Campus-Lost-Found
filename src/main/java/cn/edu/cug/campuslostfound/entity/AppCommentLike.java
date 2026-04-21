package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("app_comment_like")
// 评论点赞关系实体。
// 这张表本质上记录“哪个用户给哪条评论点过赞”，
// 主键采用 (user_id, comment_id) 组合，天然防止重复点赞记录。
public class AppCommentLike {

    private Long userId; // 点赞人用户 ID
    private Long commentId; // 被点赞的评论 / 回复 ID
}
