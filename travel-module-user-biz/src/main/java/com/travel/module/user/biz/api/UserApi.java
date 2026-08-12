package com.travel.module.user.biz.api;

import com.travel.common.core.result.ApiResult;
import com.travel.module.user.biz.api.dto.*;
import com.travel.module.user.biz.application.service.UserApplicationService;
import com.travel.module.user.biz.application.service.UserPreferenceApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 API
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserApi {
    
    private final UserApplicationService userService;
    private final UserPreferenceApplicationService preferenceService;
    
    // ==================== 用户资料 API ====================
    
    /**
     * 获取用户资料
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<ApiResult<UserProfileResponse>> getProfile(@PathVariable String userId) {
        UserProfileResponse profile = userService.getProfile(userId);
        if (profile == null) {
            return ResponseEntity.ok(ApiResult.success(null));
        }
        return ResponseEntity.ok(ApiResult.success(profile));
    }
    
    /**
     * 更新用户资料
     */
    @PutMapping("/profile/{userId}")
    public ResponseEntity<ApiResult<UserProfileResponse>> updateProfile(
            @PathVariable String userId,
            @RequestBody UserProfileRequest request) {
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResult.success(profile));
    }
    
    // ==================== 用户偏好 API ====================
    
    /**
     * 获取用户偏好
     */
    @GetMapping("/preference/{userId}")
    public ResponseEntity<ApiResult<UserPreferenceResponse>> getPreference(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "default") String type) {
        UserPreferenceResponse preference = preferenceService.getPreference(userId, type);
        return ResponseEntity.ok(ApiResult.success(preference));
    }
    
    /**
     * 获取用户所有偏好
     */
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<ApiResult<List<UserPreferenceResponse>>> getAllPreferences(@PathVariable String userId) {
        List<UserPreferenceResponse> preferences = preferenceService.getAllPreferences(userId);
        return ResponseEntity.ok(ApiResult.success(preferences));
    }
    
    /**
     * 保存用户偏好
     */
    @PostMapping("/preference/{userId}")
    public ResponseEntity<ApiResult<UserPreferenceResponse>> savePreference(
            @PathVariable String userId,
            @RequestBody UserPreferenceRequest request) {
        UserPreferenceResponse preference = preferenceService.savePreference(userId, request);
        return ResponseEntity.ok(ApiResult.success(preference));
    }
    
    /**
     * 删除用户偏好
     */
    @DeleteMapping("/preference/{preferenceId}")
    public ResponseEntity<ApiResult<Void>> deletePreference(@PathVariable Long preferenceId) {
        preferenceService.deletePreference(preferenceId);
        return ResponseEntity.ok(ApiResult.success(null));
    }
    
    /**
     * 获取用户偏好摘要（供 Agent 使用）
     */
    @GetMapping("/preference/{userId}/summary")
    public ResponseEntity<ApiResult<String>> getPreferenceSummary(@PathVariable String userId) {
        String summary = preferenceService.getPreferenceSummary(userId);
        return ResponseEntity.ok(ApiResult.success(summary));
    }
}
