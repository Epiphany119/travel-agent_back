package com.travel.module.itinerary.biz.domain.entity;

import lombok.Data;

/**
 * 餐饮推荐
 */
@Data
public class MealRecommendation {

    /** 餐次：早餐/午餐/晚餐 */
    private String mealType;
    /** 餐厅名称 */
    private String restaurantName;
    /** 菜系 */
    private String cuisine;
    /** 人均价格 */
    private Double avgPrice;
    /** 地址 */
    private String address;
    /** 推荐理由 */
    private String reason;
    /** 评分 */
    private Double rating;
}
