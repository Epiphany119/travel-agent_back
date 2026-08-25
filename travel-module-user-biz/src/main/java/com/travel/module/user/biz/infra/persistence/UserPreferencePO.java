package com.travel.module.user.biz.infra.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@Data
@TableName("user_travel_preference")
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略前端传入的额外字段（如 username、name 等认证字段）
public class UserPreferencePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String preferenceType;
    private String preferenceName;
    private String favoriteDestinations;
    private String defaultDepartureCity;
    private Integer defaultDays;
    private Integer defaultBudget;
    private Integer defaultTravelers;
    private String preferredSeason;
    private String preferredSeasonDetail;
    private String preferredMonth;
    private String preferredTripType;
    private String budgetLevel;
    private Integer dailyBudgetMin;
    private Integer dailyBudgetMax;
    private String travelStyle;
    private String interests;
    private String attractionTypes;
    private Integer maxAttractionsPerDay;
    private Boolean preferFreeAttractions;
    private String dietaryRequirements;
    private String preferredCuisines;
    private String cuisinePreferences;
    private Integer mealBudgetPerPerson;
    private Integer spicyLevel;
    private String accommodationType;
    private String accommodationRequirements;
    private Integer hotelStarMin;
    private Integer hotelBudgetPerNightMin;
    private Integer hotelBudgetPerNightMax;
    private String preferredHotelType;
    private String transportationPreference;
    private String seatPreference;
    private Integer maxTransitDuration;
    private String travelCompanion;
    private Boolean hasChildren;
    private String childrenAges;
    private Boolean hasElderly;
    private Boolean hasDisability;
    private String activityLevel;
    private String pacePreference;
    private String mobilityRequirements;
    private String shoppingPreference;
    private Integer shoppingBudget;
    private String specialRequests;
    private Integer notifyBeforeTripDays;
    private Boolean notifyWeatherAlert;
    private Boolean notifyPriceChange;
    private String preferredLanguage;
    /** 系统界面主题颜色 JSON: {fg,bg,accent} */
    private String systemThemeJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
