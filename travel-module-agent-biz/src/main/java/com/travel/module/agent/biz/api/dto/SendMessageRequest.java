package com.travel.module.agent.biz.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送消息请求DTO
 */
@Data
public class SendMessageRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 附加上下文 */
    private String context;
}
