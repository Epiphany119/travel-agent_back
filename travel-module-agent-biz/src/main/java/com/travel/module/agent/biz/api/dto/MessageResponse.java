package com.travel.module.agent.biz.api.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 消息响应DTO
 */
@Data
@Builder
public class MessageResponse {

    private String messageId;
    /** 消息角色 */
    private String role;
    /** 消息内容 */
    private String content;
    /** 工具调用信息（如果有） */
    private ToolCallInfo toolCall;
    /** 是否需要调用工具 */
    private Boolean needsToolCall;

    @Data
    @Builder
    public static class ToolCallInfo {
        private String toolCallId;
        private String toolName;
        private String action;
    }
}
