package com.travel.module.agent.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 消息持久化对象
 */
@Data
@TableName("chat_message")
public class ChatMessagePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;
    private String sessionId;
    private String role;
    private String content;
    private String toolCallId;
    private String toolName;
    private LocalDateTime createdAt;
}
