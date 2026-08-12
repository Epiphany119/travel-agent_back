package com.travel.module.user.biz.api.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 用户资料响应 DTO
 */
@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String userId;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String bio;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
