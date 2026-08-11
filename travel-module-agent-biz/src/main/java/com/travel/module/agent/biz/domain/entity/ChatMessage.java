package com.travel.module.agent.biz.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息值对象
 */
@Data
public class ChatMessage {

    private String messageId;
    /** 消息角色: user/assistant/system/tool */
    private String role;
    /** 消息内容 */
    private String content;
    /** 工具调用结果（当role=tool时） */
    private String toolCallId;
    /** 工具名称（当role=tool时） */
    private String toolName;
    /** 消息时间 */
    private LocalDateTime createdAt;

    public static ChatMessage user(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("user");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        return msg;
    }

    public static ChatMessage assistant(String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        return msg;
    }

    public static ChatMessage tool(String toolCallId, String toolName, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole("tool");
        msg.setToolCallId(toolCallId);
        msg.setToolName(toolName);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        return msg;
    }
}
