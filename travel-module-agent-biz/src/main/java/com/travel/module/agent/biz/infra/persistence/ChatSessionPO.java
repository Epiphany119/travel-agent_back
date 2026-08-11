package com.travel.module.agent.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 会话持久化对象
 */
@Data
@TableName("chat_session")
public class ChatSessionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private Long userId;
    private String title;
    private String status;
    private String preferenceJson;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
