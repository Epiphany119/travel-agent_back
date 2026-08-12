package com.travel.module.user.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("journey")
public class JourneyPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String destination;
    private String departureCity;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private String summary;
    private Integer totalCost;
    private Integer rating;
    private String travelType;
    private String companions;
    private String weatherInfo;
    private String highlight;
    private String tips;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}