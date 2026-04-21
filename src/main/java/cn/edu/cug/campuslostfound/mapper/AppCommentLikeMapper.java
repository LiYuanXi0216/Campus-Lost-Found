package cn.edu.cug.campuslostfound.mapper;

import cn.edu.cug.campuslostfound.entity.AppCommentLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// 点赞关系表 Mapper。
// 当前没有自定义 SQL，直接继承 BaseMapper 即可完成：
// - 新增点赞记录
// - 删除点赞记录
// - 查询某用户是否点过某评论
public interface AppCommentLikeMapper extends BaseMapper<AppCommentLike> {
}
