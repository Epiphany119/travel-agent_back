package com.travel.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送邮箱验证码请求 DTO
 *
 * @param email 目标邮箱地址
 */
public record EmailCodeRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email
) {
}
