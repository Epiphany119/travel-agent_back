package com.travel.module.user.biz.application.service;

import com.travel.module.user.biz.api.dto.UserResponse;
import com.travel.module.user.biz.domain.entity.User;
import com.travel.module.user.biz.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            // 返回默认用户
            return UserResponse.builder()
                    .id(userId)
                    .username("guest")
                    .nickname("游客")
                    .build();
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .frequentDestination(user.getFrequentDestination())
                .build();
    }
}
