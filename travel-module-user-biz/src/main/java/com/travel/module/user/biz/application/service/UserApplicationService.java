package com.travel.module.user.biz.application.service;

import com.travel.module.user.biz.api.dto.UserProfileRequest;
import com.travel.module.user.biz.api.dto.UserProfileResponse;
import com.travel.module.user.biz.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {
    
    private final UserRepository userRepository;
    
    /**
     * 获取用户资料
     */
    public UserProfileResponse getProfile(String userId) {
        var user = userRepository.findByUserId(userId);
        if (user == null) {
            return null;
        }
        return toResponse(user);
    }
    
    /**
     * 更新用户资料
     */
    public UserProfileResponse updateProfile(String userId, UserProfileRequest request) {
        var user = userRepository.findByUserId(userId);
        if (user == null) {
            user = new com.travel.module.user.biz.domain.entity.User();
            user.setId(Long.parseLong(userId));
        }
        
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        
        user = userRepository.save(user);
        log.info("更新用户资料成功: userId={}", userId);
        
        return toResponse(user);
    }
    
    private UserProfileResponse toResponse(com.travel.module.user.biz.domain.entity.User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .userId(String.valueOf(user.getId()))
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(1)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
