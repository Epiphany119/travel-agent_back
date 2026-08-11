package com.travel.module.user.biz.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息实体
 */
@Data
public class User {

    private Long id;
    /** 用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 头像URL */
    private String avatar;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 常用目的地 */
    private String frequentDestination;
    /** 旅行偏好设定 */
    private String defaultPreferences;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
