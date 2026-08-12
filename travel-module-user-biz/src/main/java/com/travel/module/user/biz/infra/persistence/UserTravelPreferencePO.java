package com.travel.module.user.biz.infra.persistence;

import com.travel.module.user.biz.domain.entity.UserTravelPreference;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户旅行偏好持久化对象
 */
@Data
public class UserTravelPreferencePO {
    
    private Long id;
    private String userId;
    private String preferenceType;
    private String preferenceName;
    
    // 目的地偏好
    private String favoriteDestinations;
    private String preferredSeason;
    
    // 预算偏好
    private String budgetLevel;
    private Integer dailyBudgetMin;
    private Integer dailyBudgetMax;
    
    // 旅行风格
    private String travelStyle;
    private String interests;
    
    // 饮食偏好
    private String dietaryRequirements;
    private String preferredCuisines;
    
    // 住宿偏好
    private String accommodationType;
    private String accommodationRequirements;
    
    // 交通偏好
    private String transportationPreference;
    
    // 同行偏好
    private String travelCompanion;
    private Boolean hasChildren;
    private String childrenAges;
    
    // 其他偏好
    private String activityLevel;
    private String pacePreference;
    private String mobilityRequirements;
    
    // 特殊需求
    private String specialRequests;
    
    // 时间戳
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 转换为领域实体
     */
    public UserTravelPreference toEntity() {
        return UserTravelPreference.builder()
                .id(id)
                .userId(userId)
                .preferenceType(preferenceType)
                .preferenceName(preferenceName)
                .favoriteDestinations(favoriteDestinations)
                .preferredSeason(preferredSeason)
                .budgetLevel(budgetLevel)
                .dailyBudgetMin(dailyBudgetMin)
                .dailyBudgetMax(dailyBudgetMax)
                .travelStyle(travelStyle)
                .interests(interests)
                .dietaryRequirements(dietaryRequirements)
                .preferredCuisines(preferredCuisines)
                .accommodationType(accommodationType)
                .accommodationRequirements(accommodationRequirements)
                .transportationPreference(transportationPreference)
                .travelCompanion(travelCompanion)
                .hasChildren(hasChildren)
                .childrenAges(childrenAges)
                .activityLevel(activityLevel)
                .pacePreference(pacePreference)
                .mobilityRequirements(mobilityRequirements)
                .specialRequests(specialRequests)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
    
    /**
     * 从领域实体转换
     */
    public static UserTravelPreferencePO fromEntity(UserTravelPreference entity) {
        UserTravelPreferencePO po = new UserTravelPreferencePO();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setPreferenceType(entity.getPreferenceType());
        po.setPreferenceName(entity.getPreferenceName());
        po.setFavoriteDestinations(entity.getFavoriteDestinations());
        po.setPreferredSeason(entity.getPreferredSeason());
        po.setBudgetLevel(entity.getBudgetLevel());
        po.setDailyBudgetMin(entity.getDailyBudgetMin());
        po.setDailyBudgetMax(entity.getDailyBudgetMax());
        po.setTravelStyle(entity.getTravelStyle());
        po.setInterests(entity.getInterests());
        po.setDietaryRequirements(entity.getDietaryRequirements());
        po.setPreferredCuisines(entity.getPreferredCuisines());
        po.setAccommodationType(entity.getAccommodationType());
        po.setAccommodationRequirements(entity.getAccommodationRequirements());
        po.setTransportationPreference(entity.getTransportationPreference());
        po.setTravelCompanion(entity.getTravelCompanion());
        po.setHasChildren(entity.getHasChildren());
        po.setChildrenAges(entity.getChildrenAges());
        po.setActivityLevel(entity.getActivityLevel());
        po.setPacePreference(entity.getPacePreference());
        po.setMobilityRequirements(entity.getMobilityRequirements());
        po.setSpecialRequests(entity.getSpecialRequests());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }
}
