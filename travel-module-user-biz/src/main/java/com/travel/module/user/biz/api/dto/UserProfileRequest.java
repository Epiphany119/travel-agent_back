package com.travel.module.user.biz.api.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 用户资料请求 DTO
 */
@Data
@Builder
public class UserProfileRequest {
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String bio;
}
