package com.travel.auth.api.dto;

/**
 * 认证成功响应 DTO
 *
 * @param token     会话 Token
 * @param userId    用户 ID
 * @param username  用户名
 * @param expiresIn 过期时间（秒）
 */
public record AuthTokenResponse(
        String token,
        String userId,
        String username,
        long expiresIn
) {
}
