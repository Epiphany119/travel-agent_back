package com.travel.module.itinerary.biz.domain.entity;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 旅行行程聚合根
 */
@Data
public class Itinerary {

    private Long id;
    private String itineraryId;
    private String sessionId;
    private Long userId;
    /** 目的地 */
    private String destination;
    /** 开始日期 */
    private LocalDate startDate;
    /** 结束日期 */
    private LocalDate endDate;
    /** 总预算 */
    private Double totalBudget;
    /** 行程天数 */
    private Integer days;
    /** 日程安排 */
    private List<DayPlan> dayPlans;
    /** 创建时间 */
    private java.time.LocalDateTime createdAt;
    /** 更新时间 */
    private java.time.LocalDateTime updatedAt;

    /**
     * 计算行程天数
     */
    public int calculateDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
