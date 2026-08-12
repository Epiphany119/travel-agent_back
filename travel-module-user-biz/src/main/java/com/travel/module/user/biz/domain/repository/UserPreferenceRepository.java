package com.travel.module.user.biz.domain.repository;

import com.travel.module.user.biz.domain.entity.UserTravelPreference;
import java.util.List;

/**
 * 用户偏好仓储接口
 */
public interface UserPreferenceRepository {
    
    /**
     * 根据用户ID和偏好类型查询偏好
     */
    UserTravelPreference findByUserIdAndType(String userId, String preferenceType);
    
    /**
     * 根据用户ID查询所有偏好
     */
    List<UserTravelPreference> findByUserId(String userId);
    
    /**
     * 保存偏好（新增或更新）
     */
    UserTravelPreference save(UserTravelPreference preference);
    
    /**
     * 删除偏好
     */
    void deleteById(Long id);
    
    /**
     * 获取用户的默认偏好
     */
    default UserTravelPreference findDefaultPreference(String userId) {
        return findByUserIdAndType(userId, "default");
    }
}
