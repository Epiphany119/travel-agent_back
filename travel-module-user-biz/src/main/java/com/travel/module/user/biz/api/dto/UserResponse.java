package com.travel.module.user.biz.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String frequentDestination;
}
