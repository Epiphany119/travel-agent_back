package com.travel.module.user.biz.api;

import com.travel.common.core.result.ApiResult;
import com.travel.module.user.biz.api.dto.UserResponse;
import com.travel.module.user.biz.application.service.UserApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserApi {

    private final UserApplicationService userService;

    @GetMapping("/{userId}")
    public ApiResult<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResult.success(userService.getUserById(userId));
    }
}
