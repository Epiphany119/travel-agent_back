package com.travel.module.agent.biz.api.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 会话响应DTO
 */
@Data
@Builder
public class SessionResponse {

    private String sessionId;
    private String title;
    private String status;
    private String lastMessage;
    private String createdAt;
    private String lastActiveAt;
}
