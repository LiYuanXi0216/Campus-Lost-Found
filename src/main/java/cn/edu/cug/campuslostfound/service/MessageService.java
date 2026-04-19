package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.Message;
import cn.edu.cug.campuslostfound.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;

    // 通用推消息方法
    public void pushMessage(Long userId, String type, String title, String content, Long relatedId) {
        Message msg = new Message();
        msg.setUserId(userId);
        msg.setType(type);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setRelatedId(relatedId);
        msg.setIsRead(false);
        msg.setCreateTime(java.time.LocalDateTime.now());
        messageMapper.insert(msg);
    }

    // 获取我的消息列表
    public List<Message> getMyMessages(Long userId) {
        return messageMapper.selectList(new QueryWrapper<Message>()
                .eq("user_id", userId).orderByDesc("create_time"));
    }

    // 标记已读
    public void markAsRead(Long msgId, Long userId) {
        Message msg = messageMapper.selectById(msgId);
        if (msg != null && msg.getUserId().equals(userId)) {
            msg.setIsRead(true);
            messageMapper.updateById(msg);
        }
    }
}
