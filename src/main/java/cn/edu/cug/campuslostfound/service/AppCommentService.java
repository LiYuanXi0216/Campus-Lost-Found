package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.AppComment;
import cn.edu.cug.campuslostfound.entity.AppCommentLike;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.AppCommentMapper;
import cn.edu.cug.campuslostfound.mapper.AppCommentLikeMapper;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
// 评论服务负责承接评论模块的核心业务：
// 1. 新增顶级评论 / 新增回复
// 2. 查询评论树，并补齐前端展示字段
// 3. 点赞 / 取消点赞
// 4. 给帖子作者、被回复者推送站内消息
//
// 设计上，这里把“数据库里如何存”和“前端最终如何展示”接起来：
// - 数据库存的是评论平铺表 + rootCommentId / parentCommentId
// - 返回前会重新组装成前端更容易消费的树结构
public class AppCommentService {

    private final AppCommentMapper commentMapper;
    private final AppCommentLikeMapper commentLikeMapper;
    private final ItemPostMapper postMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    public AppCommentService(AppCommentMapper commentMapper,
                             AppCommentLikeMapper commentLikeMapper,
                             ItemPostMapper postMapper,
                             UserMapper userMapper,
                             MessageService messageService) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
    }

    // 功能 1：新增评论/回复
    // 整个流程在事务里完成，避免出现：
    // - 评论已经写入，但 rootCommentId 还没回填
    // - 评论写入了，但通知没推成功导致数据半成品
    @Transactional
    public AppComment createComment(AppComment input, Long currentUserId) {
        // 1. 先做基础校验：评论至少要有文字或图片，不能发送空评论
        boolean hasText = StringUtils.hasText(input.getContent());
        boolean hasImage = StringUtils.hasText(input.getImageUrl());
        if (!hasText && !hasImage) {
            throw new RuntimeException("评论内容和图片不能都为空");
        }

        // 2. 帖子必须真实存在，避免脏数据挂到空气上
        ItemPost post = postMapper.selectById(input.getPostId());
        if (post == null) {
            throw new RuntimeException("帖子不存在，无法评论");
        }

        // 3. 创建统一的评论实体
        // 注意这里不会直接信任前端传来的 isReply / rootCommentId 等字段，
        // 而是统一由后端根据 parentCommentId 重新推导，避免脏数据。
        AppComment comment = new AppComment();
        comment.setPublisherId(currentUserId);
        comment.setPostId(input.getPostId());
        comment.setContent(hasText ? input.getContent().trim() : null);
        comment.setImageUrl(input.getImageUrl());
        comment.setCreateTime(LocalDateTime.now());
        comment.setLikeCount(0);

        // 4. 如果带了 parentCommentId，就说明这是一次回复
        // 这里的关键是 rootCommentId：
        // - 顶级评论的 rootCommentId = 自己的 id
        // - 任何层级的回复都挂到同一个 rootCommentId 下
        //
        // 这样查询时只要按帖子把评论全量取出，再按 rootCommentId 分组，
        // 就能很快恢复出“某条顶级评论 + 其所有回复”的树。
        if (input.getParentCommentId() != null) {
            AppComment parent = commentMapper.selectById(input.getParentCommentId());
            if (parent == null || !parent.getPostId().equals(input.getPostId())) {
                throw new RuntimeException("回复目标不存在，或不属于当前帖子");
            }

            comment.setIsReply(true);
            comment.setParentCommentId(parent.getId());
            // rootCommentId 统一指向顶层评论，方便一次把整个评论串查出来
            comment.setRootCommentId(parent.getRootCommentId() != null ? parent.getRootCommentId() : parent.getId());
        } else {
            comment.setIsReply(false);
        }

        commentMapper.insert(comment);

        // 5. 对于不属于回复的评论，置 rootCommentId = 自己，后续所有回复都挂在它下面
        if (!Boolean.TRUE.equals(comment.getIsReply())) {
            comment.setRootCommentId(comment.getId());
            commentMapper.updateById(comment);
        }

        // 6. 通知相关人员：评论通知帖子作者，回复通知被回复的人
        pushCommentNotification(comment, post);

        // 7. 返回前补全展示字段，前端新增后可以直接渲染
        // 这里返回的是“带展示信息的评论对象”，而不是数据库里的最简结构。
        // 这样前端提交成功后，如果想局部更新，不必再额外查一次用户昵称 / 头像。
        fillUserInfo(comment, new HashMap<>());
        if (comment.getParentCommentId() != null) {
            AppComment parent = commentMapper.selectById(comment.getParentCommentId());
            if (parent != null) {
                User parentUser = userMapper.selectById(parent.getPublisherId());
                if (parentUser != null) {
                    comment.setReplyToUserName(resolveDisplayName(parentUser));
                }
            }
        }
        return comment;
    }

    // 功能 2：按帖子查询评论树，并支持按点赞/发布时间排序
    // 返回值不是数据库里的平铺列表，而是：
    // [顶级评论1(replies=[]), 顶级评论2(replies=[]), ...]
    //
    // 这是详情页最适合直接渲染的结构，前端不需要再做二次组装。
    public List<AppComment> getPostComments(Long postId, String sortBy, Long currentUserId) {
        // 1. 把当前帖子的所有评论和回复一次性查出，避免前端不断追接口
        List<AppComment> allComments = commentMapper.selectList(new QueryWrapper<AppComment>()
                .eq("post_id", postId));

        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 先把涉及到的用户一次性查出来，再补充昵称和头像，载入点赞状态，减少重复查库
        Map<Long, User> userMap = buildUserMap(allComments);
        Map<Long, Boolean> likeStateMap = buildLikeStateMap(allComments, currentUserId);

        // 3. 顶级评论单独放一组；rootCommentId -> 子回复列表，用于组装树结构
        // repliesMap 的含义是：
        // key   = 顶级评论 id
        // value = 这条评论串下面的所有回复
        Map<Long, List<AppComment>> repliesMap = new LinkedHashMap<>();
        List<AppComment> topLevelComments = new ArrayList<>();

        for (AppComment comment : allComments) {
            fillUserInfo(comment, userMap);
            comment.setLikedByCurrentUser(Boolean.TRUE.equals(likeStateMap.get(comment.getId())));
            comment.setReplies(new ArrayList<>()); // 防止旧数据残留

            if (Boolean.TRUE.equals(comment.getIsReply())) {
                Long rootCommentId = comment.getRootCommentId();
                if (rootCommentId != null) {
                    repliesMap.computeIfAbsent(rootCommentId, key -> new ArrayList<>()).add(comment);
                }
            } else {
                topLevelComments.add(comment);
            }
        }

        // 4. 针对每条回复补上“回复某某”的展示信息
        // 例如：
        // A 发表顶级评论
        // B 回复 A
        // C 回复 B
        //
        // 那么前端展示时，B 和 C 都需要出现“回复 xxx”的文字提示，
        // 所以这里按 parentCommentId 找到被直接回复的人，再写进 replyToUserName。
        for (List<AppComment> replies : repliesMap.values()) {
            for (AppComment reply : replies) {
                if (reply.getParentCommentId() != null) {
                    AppComment parent = findCommentById(allComments, reply.getParentCommentId());
                    if (parent != null) {
                        reply.setReplyToUserName(parent.getPublisherName());
                    }
                }
            }
            replies.sort(buildComparator(sortBy));
        }

        // 5. 将回复挂回对应的顶级评论下方，前端直接按树来渲染即可
        // 当前实现是“单层 replies 列表”，而不是无限嵌套树。
        // 这样前端结构更稳定，也更符合常见社交产品的评论样式。
        for (AppComment topLevelComment : topLevelComments) {
            List<AppComment> replies = repliesMap.get(topLevelComment.getId());
            if (replies != null) {
                topLevelComment.setReplies(replies);
            }
        }

        // 6. 最后统一给顶级评论排序
        topLevelComments.sort(buildComparator(sortBy));
        return topLevelComments;
    }

    // 功能 3：给评论或回复点赞
    // 点赞关系单独存表 app_comment_like，原因是：
    // 1. 需要知道“这个用户是否点过赞”
    // 2. 需要避免同一用户重复点赞
    // 3. 后续如果要做点赞列表、点赞通知，也更容易扩展
    @Transactional
    public AppComment likeComment(Long commentId, Long currentUserId) {
        AppComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        AppCommentLike existingLike = commentLikeMapper.selectOne(new QueryWrapper<AppCommentLike>()
                .eq("user_id", currentUserId)
                .eq("comment_id", commentId)
                .last("LIMIT 1"));

        // existingLike 是否存在，决定本次是“点赞”还是“取消点赞”。
        boolean likedByCurrentUser;
        if (existingLike == null) {
            commentLikeMapper.insert(new AppCommentLike(currentUserId, commentId));
            commentMapper.update(null, new UpdateWrapper<AppComment>()
                    .eq("id", commentId)
                    .setSql("like_count = COALESCE(like_count, 0) + 1"));
            likedByCurrentUser = true;
        } else {
            commentLikeMapper.delete(new QueryWrapper<AppCommentLike>()
                    .eq("user_id", currentUserId)
                    .eq("comment_id", commentId));
            commentMapper.update(null, new UpdateWrapper<AppComment>()
                    .eq("id", commentId)
                    .setSql("like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)"));
            likedByCurrentUser = false;
        }

        // 再查一次最新评论，是为了拿到数据库里更新后的 like_count。
        comment = commentMapper.selectById(commentId);

        Map<Long, User> userMap = buildUserMap(List.of(comment));
        fillUserInfo(comment, userMap);
        comment.setLikedByCurrentUser(likedByCurrentUser);
        return comment;
    }

    // 功能 4：统计某个用户发过多少条评论（个人中心展示使用）
    public long countCommentsByUser(Long userId) {
        return commentMapper.selectCount(new QueryWrapper<AppComment>().eq("publisher_id", userId));
    }

    // 组装排序器：支持 latest(按时间倒序) 和 likes(按点赞数倒序)
    // 当点赞数相同的时候，再按发布时间倒序兜底，保证排序结果稳定。
    private Comparator<AppComment> buildComparator(String sortBy) {
        if ("likes".equalsIgnoreCase(sortBy)) {
            return Comparator
                    .comparing((AppComment comment) -> comment.getLikeCount() == null ? 0 : comment.getLikeCount())
                    .reversed()
                    .thenComparing(AppComment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(AppComment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    // 给评论填充用户显示信息，避免前端只拿到 ID 什么都显示不出来。
    // 这一步故意放在后端做，是为了把“昵称回退到用户名”“头像缺省值”这些规则统一起来。
    private void fillUserInfo(AppComment comment, Map<Long, User> userMap) {
        User user = userMap.get(comment.getPublisherId());
        if (user == null) {
            user = userMapper.selectById(comment.getPublisherId());
            if (user != null) {
                userMap.put(user.getId(), user);
            }
        }

        if (user != null) {
            comment.setPublisherName(resolveDisplayName(user));
            comment.setPublisherAvatar(resolveAvatar(user));
        } else {
            comment.setPublisherName("未知用户");
            comment.setPublisherAvatar("https://api.dicebear.com/7.x/pixel-art/svg?seed=unknown");
        }
    }

    // 一次性构建评论作者缓存，减少 N+1 查询。
    // 这里虽然还是按 id 单条 select，但至少在同一轮树组装里不会重复查同一个用户。
    private Map<Long, User> buildUserMap(List<AppComment> comments) {
        Map<Long, User> userMap = new HashMap<>();
        for (AppComment comment : comments) {
            if (comment.getPublisherId() == null || userMap.containsKey(comment.getPublisherId())) {
                continue;
            }
            User user = userMapper.selectById(comment.getPublisherId());
            if (user != null) {
                userMap.put(user.getId(), user);
            }
        }
        return userMap;
    }

    // 构建“当前用户对哪些评论点过赞”的映射。
    // 返回结果形如：commentId -> true
    // 前端拿到后就能直接决定点赞按钮的样式。
    private Map<Long, Boolean> buildLikeStateMap(List<AppComment> comments, Long currentUserId) {
        Map<Long, Boolean> likeStateMap = new HashMap<>();
        if (currentUserId == null || comments.isEmpty()) {
            return likeStateMap;
        }

        List<Long> commentIds = comments.stream()
                .map(AppComment::getId)
                .filter(id -> id != null)
                .toList();
        if (commentIds.isEmpty()) {
            return likeStateMap;
        }

        List<AppCommentLike> likes = commentLikeMapper.selectList(new QueryWrapper<AppCommentLike>()
                .eq("user_id", currentUserId)
                .in("comment_id", commentIds));

        for (AppCommentLike like : likes) {
            likeStateMap.put(like.getCommentId(), true);
        }
        return likeStateMap;
    }

    // 在当前帖子的一次性查询结果里按 id 查找某条评论。
    // 这里 comments 数量通常不会特别大，线性查找已经足够；
    // 如果以后评论量明显增加，可以考虑改成 Map<Long, AppComment>。
    private AppComment findCommentById(List<AppComment> comments, Long commentId) {
        for (AppComment comment : comments) {
            if (comment.getId().equals(commentId)) {
                return comment;
            }
        }
        return null;
    }

    private String resolveDisplayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String resolveAvatar(User user) {
        if (StringUtils.hasText(user.getAvatar())) {
            return user.getAvatar();
        }
        return "https://api.dicebear.com/7.x/pixel-art/svg?seed=" + user.getUsername();
    }

    // 评论成功后推送站内消息。
    // 推送策略分两类：
    // 1. 顶级评论：提醒帖子作者
    // 2. 回复：优先提醒被回复的人；如果帖子作者不是同一人，再额外提醒帖子作者
    //
    // 这么做的目的是尽量让相关人都能收到互动提醒，同时避免对同一人重复推送。
    private void pushCommentNotification(AppComment comment, ItemPost post) {
        Long currentUserId = comment.getPublisherId();

        // 顶级评论：提醒帖子作者有人发表了评论了
        if (!Boolean.TRUE.equals(comment.getIsReply())) {
            Long postOwnerId = Long.valueOf(post.getPublisherId());
            if (!postOwnerId.equals(currentUserId)) {
                messageService.pushMessage(
                        postOwnerId,
                        "COMMENT",
                        "您的帖子收到了新评论",
                        "帖子《" + post.getTitle() + "》收到了一条新评论，请及时查看。",
                        post.getId()
                );
            }
            return;
        }

        // 回复：优先提醒被回复的那个人
        AppComment parent = commentMapper.selectById(comment.getParentCommentId());
        if (parent != null && !parent.getPublisherId().equals(currentUserId)) {
            messageService.pushMessage(
                    parent.getPublisherId(),
                    "COMMENT",
                    "有人回复了您的评论",
                    "您在帖子《" + post.getTitle() + "》下的评论收到了新回复。",
                    post.getId()
            );
        }

        // 如果帖子作者和被回复者不是同一个人，也顺手提醒帖子作者
        Long postOwnerId = Long.valueOf(post.getPublisherId());
        if (!postOwnerId.equals(currentUserId)
                && (parent == null || !postOwnerId.equals(parent.getPublisherId()))) {
            messageService.pushMessage(
                    postOwnerId,
                    "COMMENT",
                    "您的帖子有新的回复",
                    "帖子《" + post.getTitle() + "》下出现了新的回复。",
                    post.getId()
            );
        }
    }
}
