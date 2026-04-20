package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.AppComment;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.AppCommentMapper;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
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
public class AppCommentService {

    private final AppCommentMapper commentMapper;
    private final ItemPostMapper postMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    public AppCommentService(AppCommentMapper commentMapper,
                             ItemPostMapper postMapper,
                             UserMapper userMapper,
                             MessageService messageService) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
    }

    // 功能 1：新增评论/回复
    @Transactional
    public AppComment createComment(AppComment input, Long currentUserId) {
        // 1. 先做基础校验：评论至少要有文字或图片，不能两手空空
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

        // 3. 创建统一的评论实体：评论和回复都落同一张表
        AppComment comment = new AppComment();
        comment.setPublisherId(currentUserId);
        comment.setPostId(input.getPostId());
        comment.setContent(hasText ? input.getContent().trim() : null);
        comment.setImageUrl(input.getImageUrl());
        comment.setCreateTime(LocalDateTime.now());
        comment.setLikeCount(0);

        // 4. 如果带了 parentCommentId，就说明这是一次回复
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

        // 5. 顶级评论插入后，回填 rootCommentId = 自己，后续所有回复都挂在它下面
        if (!Boolean.TRUE.equals(comment.getIsReply())) {
            comment.setRootCommentId(comment.getId());
            commentMapper.updateById(comment);
        }

        // 6. 通知相关人员：评论通知帖子作者，回复通知被回复的人
        pushCommentNotification(comment, post);

        // 7. 返回前补全展示字段，前端新增后可以直接渲染
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
    public List<AppComment> getPostComments(Long postId, String sortBy) {
        // 1. 把当前帖子的所有评论和回复一次性查出，避免前端不断追接口
        List<AppComment> allComments = commentMapper.selectList(new QueryWrapper<AppComment>()
                .eq("post_id", postId));

        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 先把涉及到的用户一次性查出来，再补充昵称和头像，减少重复查库
        Map<Long, User> userMap = buildUserMap(allComments);

        // 3. 顶级评论单独放一组；rootCommentId -> 子回复列表，用于组装树结构
        Map<Long, List<AppComment>> repliesMap = new LinkedHashMap<>();
        List<AppComment> topLevelComments = new ArrayList<>();

        for (AppComment comment : allComments) {
            fillUserInfo(comment, userMap);
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
    @Transactional
    public AppComment likeComment(Long commentId) {
        AppComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        int currentLikeCount = comment.getLikeCount() == null ? 0 : comment.getLikeCount();
        comment.setLikeCount(currentLikeCount + 1);
        commentMapper.updateById(comment);

        Map<Long, User> userMap = buildUserMap(List.of(comment));
        fillUserInfo(comment, userMap);
        return comment;
    }

    // 功能 4：统计某个用户发过多少条评论（个人中心展示使用）
    public long countCommentsByUser(Long userId) {
        return commentMapper.selectCount(new QueryWrapper<AppComment>().eq("publisher_id", userId));
    }

    // 组装排序器：支持 latest(按时间倒序) 和 likes(按点赞数倒序)
    private Comparator<AppComment> buildComparator(String sortBy) {
        if ("likes".equalsIgnoreCase(sortBy)) {
            return Comparator
                    .comparing((AppComment comment) -> comment.getLikeCount() == null ? 0 : comment.getLikeCount())
                    .reversed()
                    .thenComparing(AppComment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(AppComment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    // 给评论填充用户显示信息，避免前端只拿到 ID 什么都显示不出来
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

    // 一次性构建评论作者缓存，减少 N+1 查询
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

    private void pushCommentNotification(AppComment comment, ItemPost post) {
        Long currentUserId = comment.getPublisherId();

        // 顶级评论：提醒帖子作者有人来互动了
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
