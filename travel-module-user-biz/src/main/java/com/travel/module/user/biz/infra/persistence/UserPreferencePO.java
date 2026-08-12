package com.travel.module.user.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@Data
@TableName("user_travel_preference")
public class UserPreferencePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
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
    private Integer preferFreeAttractions;
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
    private Integer hasChildren;
    private String childrenAges;
    private Integer hasElderly;
    private Integer hasDisability;
    private String activityLevel;
    private String pacePreference;
    private String mobilityRequirements;
    private String shoppingPreference;
    private Integer shoppingBudget;
    private String specialRequests;
    private Integer notifyBeforeTripDays;
    private Integer notifyWeatherAlert;
    private Integer notifyPriceChange;
    private String preferredLanguage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}