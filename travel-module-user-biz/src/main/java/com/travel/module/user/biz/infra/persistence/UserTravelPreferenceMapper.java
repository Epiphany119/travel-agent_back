package com.travel.module.user.biz.infra.persistence;

import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 用户旅行偏好 Mapper
 */
@Mapper
public interface UserTravelPreferenceMapper {
    
    @Select("SELECT * FROM user_travel_preference WHERE user_id = #{userId} AND preference_type = #{preferenceType} LIMIT 1")
    UserTravelPreferencePO findByUserIdAndType(@Param("userId") String userId, @Param("preferenceType") String preferenceType);

    @Select("SELECT * FROM user_travel_preference WHERE user_id = #{userId}")
    List<UserTravelPreferencePO> findByUserId(@Param("userId") String userId);
    
    @Select("SELECT * FROM user_travel_preference WHERE id = #{id}")
    UserTravelPreferencePO findById(@Param("id") Long id);
    
    @Insert("INSERT INTO user_travel_preference (" +
            "user_id, name, preference_type, favorite_destinations, preferred_season, " +
            "budget_level, daily_budget_min, daily_budget_max, travel_style, interests, " +
            "dietary_requirements, preferred_cuisines, accommodation_type, accommodation_requirements, " +
            "transportation_preference, travel_companion, has_children, children_ages, " +
            "activity_level, pace_preference, mobility_requirements, special_requests" +
            ") VALUES (" +
            "#{userId}, #{name}, #{preferenceType}, #{favoriteDestinations}, #{preferredSeason}, " +
            "#{budgetLevel}, #{dailyBudgetMin}, #{dailyBudgetMax}, #{travelStyle}, #{interests}, " +
            "#{dietaryRequirements}, #{preferredCuisines}, #{accommodationType}, #{accommodationRequirements}, " +
            "#{transportationPreference}, #{travelCompanion}, #{hasChildren}, #{childrenAges}, " +
            "#{activityLevel}, #{pacePreference}, #{mobilityRequirements}, #{specialRequests}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserTravelPreferencePO po);

    @Update("UPDATE user_travel_preference SET " +
            "name = #{name}, " +
            "favorite_destinations = #{favoriteDestinations}, " +
            "preferred_season = #{preferredSeason}, " +
            "budget_level = #{budgetLevel}, " +
            "daily_budget_min = #{dailyBudgetMin}, " +
            "daily_budget_max = #{dailyBudgetMax}, " +
            "travel_style = #{travelStyle}, " +
            "interests = #{interests}, " +
            "dietary_requirements = #{dietaryRequirements}, " +
            "preferred_cuisines = #{preferredCuisines}, " +
            "accommodation_type = #{accommodationType}, " +
            "accommodation_requirements = #{accommodationRequirements}, " +
            "transportation_preference = #{transportationPreference}, " +
            "travel_companion = #{travelCompanion}, " +
            "has_children = #{hasChildren}, " +
            "children_ages = #{childrenAges}, " +
            "activity_level = #{activityLevel}, " +
            "pace_preference = #{pacePreference}, " +
            "mobility_requirements = #{mobilityRequirements}, " +
            "special_requests = #{specialRequests} " +
            "WHERE id = #{id}")
    int update(UserTravelPreferencePO po);
    
    @Delete("DELETE FROM user_travel_preference WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    @Delete("DELETE FROM user_travel_preference WHERE user_id = #{userId} AND preference_type = #{preferenceType}")
    int deleteByUserIdAndType(@Param("userId") String userId, @Param("preferenceType") String preferenceType);
}
