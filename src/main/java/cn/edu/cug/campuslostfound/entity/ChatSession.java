package cn.edu.cug.campuslostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// ChatSession.java
@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long user1Id;
    private Long user2Id;
    private Long lastMsgId;
    private LocalDateTime updateTime;
}

