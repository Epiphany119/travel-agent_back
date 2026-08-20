package com.travel.module.user.biz.domain.entity;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 用户旅行偏好实体
 */
@Data
@Builder
public class UserTravelPreference {

    private Long id;

    /** 用户名（侧边栏昵称来源） */
    private String name;

    /** 用户唯一标识 */
    private String userId;
    
    /** 偏好类型: default-默认偏好, custom-自定义偏好 */
    private String preferenceType;
    
    /** 偏好名称 */
    private String preferenceName;
    
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
    
    // === 时间戳 ===
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 获取兴趣标签列表
     */
    public String[] getInterestsArray() {
        if (interests == null || interests.isBlank()) {
            return new String[0];
        }
        return interests.split(",");
    }
    
    /**
     * 设置兴趣标签
     */
    public void setInterestsArray(String[] interestArray) {
        if (interestArray == null || interestArray.length == 0) {
            this.interests = "";
        } else {
            this.interests = String.join(",", interestArray);
        }
    }
    
    /**
     * 获取常去目的地列表
     */
    public String[] getFavoriteDestinationsArray() {
        if (favoriteDestinations == null || favoriteDestinations.isBlank()) {
            return new String[0];
        }
        return favoriteDestinations.split(",");
    }
    
    /**
     * 生成偏好摘要（用于 Agent 上下文）
     */
    public String toPreferenceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户旅行偏好摘要】\n");
        
        if (favoriteDestinations != null && !favoriteDestinations.isBlank()) {
            sb.append("- 常去目的地: ").append(favoriteDestinations).append("\n");
        }
        if (preferredSeason != null && !preferredSeason.isBlank()) {
            sb.append("- 偏好季节: ").append(preferredSeason).append("\n");
        }
        if (budgetLevel != null && !budgetLevel.isBlank()) {
            sb.append("- 预算等级: ").append(getBudgetLevelText()).append("\n");
            if (dailyBudgetMin != null && dailyBudgetMax != null) {
                sb.append("- 日预算: ").append(dailyBudgetMin).append("-").append(dailyBudgetMax).append("元\n");
            }
        }
        if (travelStyle != null && !travelStyle.isBlank()) {
            sb.append("- 旅行风格: ").append(travelStyle).append("\n");
        }
        if (interests != null && !interests.isBlank()) {
            sb.append("- 兴趣标签: ").append(interests).append("\n");
        }
        if (dietaryRequirements != null && !dietaryRequirements.isBlank()) {
            sb.append("- 饮食要求: ").append(dietaryRequirements).append("\n");
        }
        if (preferredCuisines != null && !preferredCuisines.isBlank()) {
            sb.append("- 偏好菜系: ").append(preferredCuisines).append("\n");
        }
        if (accommodationType != null && !accommodationType.isBlank()) {
            sb.append("- 住宿偏好: ").append(accommodationType).append("\n");
        }
        if (transportationPreference != null && !transportationPreference.isBlank()) {
            sb.append("- 交通偏好: ").append(transportationPreference).append("\n");
        }
        if (travelCompanion != null && !travelCompanion.isBlank()) {
            sb.append("- 出行人群: ").append(getTravelCompanionText()).append("\n");
        }
        if (activityLevel != null && !activityLevel.isBlank()) {
            sb.append("- 活动强度: ").append(getActivityLevelText()).append("\n");
        }
        if (specialRequests != null && !specialRequests.isBlank()) {
            sb.append("- 特殊要求: ").append(specialRequests).append("\n");
        }
        
        return sb.toString();
    }
    
    private String getBudgetLevelText() {
        return switch (budgetLevel) {
            case "economy" -> "经济型";
            case "standard" -> "标准型";
            case "luxury" -> "豪华型";
            default -> budgetLevel;
        };
    }
    
    private String getTravelCompanionText() {
        return switch (travelCompanion) {
            case "solo" -> "独自旅行";
            case "couple" -> "情侣/夫妻";
            case "family" -> "家庭出游";
            case "friends" -> "朋友同行";
            case "business" -> "商务出行";
            default -> travelCompanion;
        };
    }
    
    private String getActivityLevelText() {
        return switch (activityLevel) {
            case "relaxed" -> "休闲";
            case "moderate" -> "适中";
            case "active" -> "活跃";
            default -> activityLevel;
        };
    }
}
