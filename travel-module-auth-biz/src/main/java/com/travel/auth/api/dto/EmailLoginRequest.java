package com.travel.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 邮箱验证码登录请求 DTO
 * <p>
 * 如果邮箱已注册则登录，未注册则自动创建新账号（用户名即邮箱）。
 *
 * @param email 邮箱地址
 * @param code  验证码
 */
public record EmailLoginRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,
        @NotBlank(message = "验证码不能为空")
        String code
) {
}
