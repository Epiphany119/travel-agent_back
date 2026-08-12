package com.travel.module.user.biz.domain.repository;

import com.travel.module.user.biz.domain.entity.User;

/**
 * 用户仓储接口
 */
public interface UserRepository {
    
    /**
     * 根据用户ID查询用户
     */
    User findById(Long userId);
    
    /**
     * 根据用户标识查询用户
     */
    User findByUserId(String userId);
    
    /**
     * 保存用户
     */
    User save(User user);
}
