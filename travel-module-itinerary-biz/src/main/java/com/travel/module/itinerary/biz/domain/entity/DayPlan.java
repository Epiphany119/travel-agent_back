package com.travel.module.itinerary.biz.domain.entity;

import lombok.Data;
import java.util.List;

/**
 * 每日行程
 */
@Data
public class DayPlan {

    /** 第几天 */
    private Integer dayNumber;
    /** 日期 */
    private String date;
    /** 主题 */
    private String theme;
    /** 景点列表 */
    private List<AttractionVisit> attractions;
    /** 餐饮推荐 */
    private List<MealRecommendation> meals;
    /** 交通建议 */
    private String transportation;
    /** 注意事项 */
    private String notes;
    /** 当日预算 */
    private Double dayBudget;
}
