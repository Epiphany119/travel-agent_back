package com.travel.auth.api;

import com.travel.auth.api.dto.*;
import com.travel.auth.application.AuthApplicationService;
import com.travel.common.core.result.ApiResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - 注册、登录、邮箱验证码、会话验证
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService auth;

    public AuthController(AuthApplicationService auth) {
        this.auth = auth;
    }

    /** 用户注册 */
    @PostMapping("/register")
    public ApiResult<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.success(auth.register(request));
    }

    /** 密码登录 */
    @PostMapping("/login")
    public ApiResult<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(auth.login(request));
    }

    /** 发送邮箱验证码 */
    @PostMapping("/email/send-code")
    public ApiResult<Void> sendCode(@Valid @RequestBody EmailCodeRequest request) {
        auth.sendCode(request.email());
        return ApiResult.success();
    }

    /**
     * 邮箱验证码登录
     * 如果邮箱未注册则自动创建新账号
     */
    @PostMapping("/email/login")
    public ApiResult<AuthTokenResponse> emailLogin(@Valid @RequestBody EmailLoginRequest request) {
        return ApiResult.success(auth.emailLogin(request));
    }

    /** 绑定邮箱（需要已登录） */
    @PostMapping("/bind-email")
    public ApiResult<Void> bindEmail(
            @Valid @RequestBody BindEmailRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        String userId = auth.verify(extractToken(header));
        if (userId == null) {
            return ApiResult.error(401, "请先登录");
        }
        auth.bindEmail(userId, request);
        return ApiResult.success();
    }

    /** 解绑邮箱（需要已登录） */
    @PostMapping("/unbind-email")
    public ApiResult<Void> unbindEmail(
            @RequestHeader(value = "Authorization", required = false) String header) {
        String userId = auth.verify(extractToken(header));
        if (userId == null) {
            return ApiResult.error(401, "请先登录");
        }
        auth.unbindEmail(userId);
        return ApiResult.success();
    }

    /** 登出 */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        auth.logout(extractToken(header));
        return ApiResult.success();
    }

    /** 校验当前 Token 是否有效 */
    @GetMapping("/verify")
    public ApiResult<Object> verify(@RequestHeader(value = "Authorization", required = false) String header) {
        String userId = auth.verify(extractToken(header));
        if (userId == null) {
            return ApiResult.success(Map.of("authenticated", false));
        }
        return ApiResult.success(Map.of("authenticated", true, "userId", userId));
    }

    private String extractToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}
