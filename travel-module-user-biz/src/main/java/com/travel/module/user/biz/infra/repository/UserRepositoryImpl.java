package com.travel.module.user.biz.infra.repository;

import com.travel.module.user.biz.domain.entity.User;
import com.travel.module.user.biz.domain.repository.UserRepository;
import com.travel.module.user.biz.infra.persistence.UserProfileMapper;
import com.travel.module.user.biz.infra.persistence.UserProfilePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    
    private final UserProfileMapper mapper;
    
    @Override
    public User findById(Long userId) {
        UserProfilePO po = mapper.findById(userId);
        return po != null ? toEntity(po) : null;
    }
    
    @Override
    public User findByUserId(String userId) {
        UserProfilePO po = mapper.findByUserId(userId);
        return po != null ? toEntity(po) : null;
    }
    
    @Override
    public User save(User user) {
        UserProfilePO existing = mapper.findByUserId(user.getUsername());
        
        if (existing != null) {
            // 更新
            existing.setNickname(user.getNickname());
            existing.setAvatar(user.getAvatar());
            existing.setEmail(user.getEmail());
            existing.setPhone(user.getPhone());
            existing.setUpdatedAt(LocalDateTime.now());
            mapper.updateByUserId(existing);
            return user;
        } else {
            // 新增
            UserProfilePO po = new UserProfilePO();
            String newUserId = user.getId() != null ? String.valueOf(user.getId()) : UUID.randomUUID().toString();
            po.setUserId(newUserId);
            po.setUsername(user.getUsername() != null ? user.getUsername() : newUserId);
            po.setNickname(user.getNickname());
            po.setAvatar(user.getAvatar());
            po.setEmail(user.getEmail());
            po.setPhone(user.getPhone());
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());
            mapper.insert(po);
            user.setId(Long.parseLong(newUserId.replace("user_", "")));
            return user;
        }
    }
    
    private User toEntity(UserProfilePO po) {
        User user = new User();
        user.setId(1L); // 默认值
        user.setUsername(po.getUsername());
        user.setNickname(po.getNickname());
        user.setAvatar(po.getAvatar());
        user.setEmail(po.getEmail());
        user.setPhone(po.getPhone());
        user.setCreatedAt(po.getCreatedAt());
        user.setUpdatedAt(po.getUpdatedAt());
        return user;
    }
}
