package com.travel.module.agent.biz.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建会话请求DTO
 */
@Data
public class CreateSessionRequest {

    @NotBlank(message = "目的地不能为空")
    private String destination;

    private String startDate;
    private String endDate;
    private String budgetLevel = "standard";
    private String travelType = "solo";
    private Integer travelers = 1;
}
