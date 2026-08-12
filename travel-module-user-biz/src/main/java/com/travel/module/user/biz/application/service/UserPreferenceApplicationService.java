package com.travel.module.user.biz.application.service;

import com.travel.module.user.biz.api.dto.*;
import com.travel.module.user.biz.domain.entity.UserTravelPreference;
import com.travel.module.user.biz.domain.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户偏好应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceApplicationService {
    
    private final UserPreferenceRepository preferenceRepository;
    
    /**
     * 获取用户偏好
     */
    public UserPreferenceResponse getPreference(String userId, String preferenceType) {
        if (preferenceType == null || preferenceType.isBlank()) {
            preferenceType = "default";
        }
        
        UserTravelPreference preference = preferenceRepository.findByUserIdAndType(userId, preferenceType);
        if (preference == null) {
            return null;
        }
        
        return toResponse(preference);
    }
    
    /**
     * 获取用户所有偏好
     */
    public List<UserPreferenceResponse> getAllPreferences(String userId) {
        List<UserTravelPreference> preferences = preferenceRepository.findByUserId(userId);
        return preferences.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户默认偏好
     */
    public UserPreferenceResponse getDefaultPreference(String userId) {
        UserTravelPreference preference = preferenceRepository.findDefaultPreference(userId);
        return preference != null ? toResponse(preference) : null;
    }
    
    /**
     * 保存用户偏好
     */
    public UserPreferenceResponse savePreference(String userId, UserPreferenceRequest request) {
        String preferenceType = request.getPreferenceName() != null && !request.getPreferenceName().isBlank()
                ? "custom" : "default";
        
        UserTravelPreference preference = preferenceRepository.findByUserIdAndType(userId, preferenceType);
        
        if (preference == null) {
            preference = UserTravelPreference.builder()
                    .userId(userId)
                    .preferenceType(preferenceType)
                    .preferenceName(request.getPreferenceName() != null ? request.getPreferenceName() : "默认偏好")
                    .build();
        }
        
        // 更新字段
        updatePreferenceFields(preference, request);
        
        preference = preferenceRepository.save(preference);
        log.info("保存用户偏好成功: userId={}, type={}", userId, preferenceType);
        
        return toResponse(preference);
    }
    
    /**
     * 删除用户偏好
     */
    public void deletePreference(Long preferenceId) {
        preferenceRepository.deleteById(preferenceId);
        log.info("删除用户偏好: id={}", preferenceId);
    }
    
    /**
     * 获取用户偏好的摘要文本（供 Agent 使用）
     */
    public String getPreferenceSummary(String userId) {
        UserTravelPreference preference = preferenceRepository.findDefaultPreference(userId);
        if (preference == null) {
            return "用户尚未设置旅行偏好，请询问用户的旅行需求。";
        }
        return preference.toPreferenceSummary();
    }
    
    private void updatePreferenceFields(UserTravelPreference preference, UserPreferenceRequest request) {
        preference.setFavoriteDestinations(request.getFavoriteDestinations());
        preference.setPreferredSeason(request.getPreferredSeason());
        preference.setBudgetLevel(request.getBudgetLevel());
        preference.setDailyBudgetMin(request.getDailyBudgetMin());
        preference.setDailyBudgetMax(request.getDailyBudgetMax());
        preference.setTravelStyle(request.getTravelStyle());
        preference.setInterests(request.getInterests());
        preference.setDietaryRequirements(request.getDietaryRequirements());
        preference.setPreferredCuisines(request.getPreferredCuisines());
        preference.setAccommodationType(request.getAccommodationType());
        preference.setAccommodationRequirements(request.getAccommodationRequirements());
        preference.setTransportationPreference(request.getTransportationPreference());
        preference.setTravelCompanion(request.getTravelCompanion());
        preference.setHasChildren(request.getHasChildren());
        preference.setChildrenAges(request.getChildrenAges());
        preference.setActivityLevel(request.getActivityLevel());
        preference.setPacePreference(request.getPacePreference());
        preference.setMobilityRequirements(request.getMobilityRequirements());
        preference.setSpecialRequests(request.getSpecialRequests());
    }
    
    private UserPreferenceResponse toResponse(UserTravelPreference preference) {
        return UserPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .preferenceType(preference.getPreferenceType())
                .preferenceName(preference.getPreferenceName())
                .favoriteDestinations(preference.getFavoriteDestinations())
                .preferredSeason(preference.getPreferredSeason())
                .budgetLevel(preference.getBudgetLevel())
                .budgetLevelText(getBudgetLevelText(preference.getBudgetLevel()))
                .dailyBudgetMin(preference.getDailyBudgetMin())
                .dailyBudgetMax(preference.getDailyBudgetMax())
                .travelStyle(preference.getTravelStyle())
                .interests(preference.getInterests())
                .dietaryRequirements(preference.getDietaryRequirements())
                .preferredCuisines(preference.getPreferredCuisines())
                .accommodationType(preference.getAccommodationType())
                .accommodationRequirements(preference.getAccommodationRequirements())
                .transportationPreference(preference.getTransportationPreference())
                .travelCompanion(preference.getTravelCompanion())
                .travelCompanionText(getTravelCompanionText(preference.getTravelCompanion()))
                .hasChildren(preference.getHasChildren())
                .childrenAges(preference.getChildrenAges())
                .activityLevel(preference.getActivityLevel())
                .activityLevelText(getActivityLevelText(preference.getActivityLevel()))
                .pacePreference(preference.getPacePreference())
                .mobilityRequirements(preference.getMobilityRequirements())
                .specialRequests(preference.getSpecialRequests())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
    
    private String getBudgetLevelText(String level) {
        if (level == null) return null;
        return switch (level) {
            case "economy" -> "经济型";
            case "standard" -> "标准型";
            case "luxury" -> "豪华型";
            default -> level;
        };
    }
    
    private String getTravelCompanionText(String companion) {
        if (companion == null) return null;
        return switch (companion) {
            case "solo" -> "独自旅行";
            case "couple" -> "情侣/夫妻";
            case "family" -> "家庭出游";
            case "friends" -> "朋友同行";
            case "business" -> "商务出行";
            default -> companion;
        };
    }
    
    private String getActivityLevelText(String level) {
        if (level == null) return null;
        return switch (level) {
            case "relaxed" -> "休闲";
            case "moderate" -> "适中";
            case "active" -> "活跃";
            default -> level;
        };
    }
}
