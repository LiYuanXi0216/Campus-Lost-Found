package cn.edu.cug.campuslostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.entity.PostSubscription;
import cn.edu.cug.campuslostfound.entity.User;
import cn.edu.cug.campuslostfound.mapper.PostSubscriptionMapper;
import cn.edu.cug.campuslostfound.mapper.UserMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
public class SubscriptionService {

    private final PostSubscriptionMapper subscriptionMapper;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;
    private final MessageService messageService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public SubscriptionService(PostSubscriptionMapper subscriptionMapper, UserMapper userMapper, JavaMailSender mailSender, MessageService messageService) {
        this.subscriptionMapper = subscriptionMapper;
        this.userMapper = userMapper;
        this.mailSender = mailSender;
        this.messageService = messageService;
    }

    // 功能 1：用户添加订阅规则
    public PostSubscription subscribe(Long userId, String keyword, Long buildingId) {
        PostSubscription sub = new PostSubscription();
        sub.setUserId(userId);
        sub.setKeyword(keyword);
        sub.setBuildingId(buildingId);
        sub.setIsActive(true);
        subscriptionMapper.insert(sub);
        return sub;
    }

    // 功能 2：【事件钩子】检查新帖子是否触发了别人的订阅规则！
    public void matchNewPostAndNotify(ItemPost newPost) {
        // 1. 查找所有处于活跃状态的订阅规则
        QueryWrapper<PostSubscription> wrapper = new QueryWrapper<>();
        wrapper.eq("is_active", true);

        // 如果新帖子有具体建筑，优先匹配该建筑或者没有设置建筑的全局订阅
        if (newPost.getBuildingId() != null) {
            wrapper.and(w -> w.eq("building_id", newPost.getBuildingId()).or().isNull("building_id"));
        }

        List<PostSubscription> activeSubs = subscriptionMapper.selectList(wrapper);

        // 2. 遍历规则，进行关键词碰撞
        String postContent = (newPost.getTitle() + " " + newPost.getDescription()).toLowerCase();

        for (PostSubscription sub : activeSubs) {
            // 不能自己通知自己
            if (sub.getUserId().toString().equals(newPost.getPublisherId())) continue;

            // 如果帖子的标题或内容包含了订阅的关键词
            if (postContent.contains(sub.getKeyword().toLowerCase())) {
                // 1. 发邮件
                sendMatchEmail(sub.getUserId(), newPost);

                // 2. 存入站内消息中心 (新增)
                messageService.pushMessage(
                        sub.getUserId(),
                        "SUBSCRIPTION",
                        "订阅匹配成功：" + newPost.getTitle(),
                        "为您匹配到一条符合规则的帖子，快去看看吧！",
                        newPost.getId()
                );
            }
        }
    }

    // 发送匹配成功邮件
    private void sendMatchEmail(Long targetUserId, ItemPost post) {
        User user = userMapper.selectById(targetUserId);
        if (user != null && user.getEmail() != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("【失物招领】您的订阅有了新匹配！");

            String typeStr = post.getType().equals("LOST") ? "寻物启事" : "招领启事";
            message.setText("亲爱的 " + user.getNickname() + "：\n\n" +
                    "系统刚刚收到了一条新的【" + typeStr + "】，与您订阅的关键词相匹配！\n" +
                    "帖子标题：" + post.getTitle() + "\n" +
                    "发生地点：" + (post.getLocationDesc() != null ? post.getLocationDesc() : "未提供") + "\n\n" +
                    "请尽快登录系统查看详情。祝您生活愉快！");
            try {
                mailSender.send(message);
                System.out.println("✅ 成功向 " + user.getEmail() + " 发送订阅匹配邮件！");
            } catch (Exception e) {
                System.err.println("邮件发送失败：" + e.getMessage());
            }
        }
    }
}