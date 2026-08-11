package com.travel.module.itinerary.biz.infra.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("itinerary")
public class ItineraryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itineraryId;
    private String sessionId;
    private Long userId;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalBudget;
    private Integer days;
    private String dayPlansJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}