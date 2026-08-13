package com.travel.module.user.biz.infra.persistence;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户资料持久化对象
 */
@Data
public class UserProfilePO {
    
    private Long id;
    private String userId;
    private String username;
    private String nickname;
    private String avatar;
    private String avatarUrl;
    private String email;
    private String phone;
    private String bio;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
