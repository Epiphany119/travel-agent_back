package com.travel.module.agent.biz.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 对话会话聚合根
 */
@Data
public class ChatSession {

    private Long id;
    /** 会话唯一标识 */
    private String sessionId;
    /** 用户ID */
    private Long userId;
    /** 会话标题（由AI生成或用户自定义） */
    private String title;
    /** 消息列表 */
    private List<ChatMessage> messages;
    /** 旅行偏好上下文 */
    private TravelPreference preference;
    /** 会话状态 */
    private String status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;

    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 更新偏好
     */
    public void updatePreference(TravelPreference preference) {
        this.preference = preference;
    }
}
