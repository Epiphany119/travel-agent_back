package com.travel.module.user.biz.api.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 用户偏好响应 DTO
 */
@Data
@Builder
public class UserPreferenceResponse {
    
    private Long id;
    private String userId;
    private String preferenceType;
    private String preferenceName;
    
    // === 目的地偏好 ===
    private String favoriteDestinations;
    private String preferredSeason;
    
    // === 预算偏好 ===
    private String budgetLevel;
    private String budgetLevelText;
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
    private String travelCompanionText;
    private Boolean hasChildren;
    private String childrenAges;
    
    // === 其他偏好 ===
    private String activityLevel;
    private String activityLevelText;
    private String pacePreference;
    private String mobilityRequirements;
    
    // === 特殊需求 ===
    private String specialRequests;
    
    // === 时间戳 ===
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 获取兴趣标签数组
     */
    public String[] getInterestsArray() {
        if (interests == null || interests.isBlank()) {
            return new String[0];
        }
        return interests.split(",");
    }
    
    /**
     * 获取常去目的地数组
     */
    public String[] getFavoriteDestinationsArray() {
        if (favoriteDestinations == null || favoriteDestinations.isBlank()) {
            return new String[0];
        }
        return favoriteDestinations.split(",");
    }
}
