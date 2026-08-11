package com.travel.module.agent.biz.domain.entity;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

/**
 * 旅行偏好值对象
 */
@Data
@Builder
public class TravelPreference {

    /** 目的地 */
    private String destination;
    /** 出发日期 */
    private LocalDate startDate;
    /** 结束日期 */
    private LocalDate endDate;
    /** 预算等级: economy/standard/luxury */
    private String budgetLevel;
    /** 旅行类型: solo/couple/family/friends/business */
    private String travelType;
    /** 同行人数 */
    private Integer travelers;
    /** 偏好景点类型 */
    private String[] interests;
    /** 饮食要求 */
    private String[] dietaryRequirements;
    /** 住宿偏好 */
    private String accommodationPreference;
    /** 交通偏好 */
    private String transportationPreference;
    /** 其他特殊要求 */
    private String specialRequests;

    /**
     * 计算旅行天数
     */
    public int getDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
