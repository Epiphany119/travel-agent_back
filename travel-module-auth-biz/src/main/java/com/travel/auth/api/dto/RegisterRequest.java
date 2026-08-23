package com.travel.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求 DTO
 *
 * @param username        用户名（必填，3-32 位字母、数字或下划线）
 * @param password        密码（必填，至少 8 位，含大小写字母和数字）
 * @param confirmPassword 确认密码（必填，需与 password 一致）
 * @param email           邮箱（可选，填写时需完成邮箱验证）
 * @param emailCode       邮箱验证码（email 非空时必填）
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "请再次输入密码") String confirmPassword,
        @Email(message = "邮箱格式不正确") String email,
        String emailCode
) {
}
