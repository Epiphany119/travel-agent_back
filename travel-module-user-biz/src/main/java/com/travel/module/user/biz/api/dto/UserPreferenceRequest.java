package com.travel.module.user.biz.api.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 用户偏好请求 DTO
 */
@Data
@Builder
public class UserPreferenceRequest {

    // === 基础信息 ===
    private String name;
    private String email;
    private String phone;

    // === 目的地偏好 ===
    private String favoriteDestinations;
    private String preferredSeason;

    // === 预算偏好 ===
    private String budgetLevel;
    private Integer dailyBudgetMin;
    private Integer dailyBudgetMax;

    // === 旅行风格 ===
    private String travelStyle;
    private String interests;

    // === 饮食偏好 ===
    private String dietaryRequirements;
    private String preferredCuisines;

    // === 住宿偏好 ===
    private String accommodationType;
    private String accommodationRequirements;

    // === 交通偏好 ===
    private String transportationPreference;

    // === 同行偏好 ===
    private String travelCompanion;
    private Boolean hasChildren;
    private String childrenAges;

    // === 其他偏好 ===
    private String activityLevel;
    private String pacePreference;
    private String mobilityRequirements;

    // === 特殊需求 ===
    private String specialRequests;

    // === 偏好配置 ===
    private String preferenceName;
}
