package com.travel.module.user.biz.api;

import com.travel.common.core.result.ApiResult;
import com.travel.module.user.biz.domain.service.UserBizService;
import com.travel.module.user.biz.infra.persistence.InspirationPO;
import com.travel.module.user.biz.infra.persistence.JourneyPO;
import com.travel.module.user.biz.infra.persistence.JourneyPointPO;
import com.travel.module.user.biz.infra.persistence.JourneyImagePO;
import com.travel.module.user.biz.infra.persistence.UserPreferencePO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserApi {
    
    private final UserBizService userBizService;

    @GetMapping("/test")
    public ApiResult<?> test() {
        return ApiResult.success("UserApi is working!");
    }

    // ========== 灵感目的地 ==========
    @GetMapping("/inspirations")
    public ApiResult<?> listInspirations(@RequestParam(defaultValue = "user_001") String userId) {
        List<InspirationPO> list = userBizService.listInspirations(userId);
        return ApiResult.success(list);
    }

    @PostMapping("/inspirations")
    public ApiResult<?> addInspiration(@RequestBody InspirationPO po) {
        if (po.getUserId() == null) po.setUserId("user_001");
        userBizService.addInspiration(po);
        return ApiResult.success(po);
    }

    @PutMapping("/inspirations/{id}")
    public ApiResult<?> updateInspiration(@PathVariable Long id, @RequestBody InspirationPO po) {
        po.setId(id);
        userBizService.updateInspiration(po);
        return ApiResult.success("OK");
    }

    @DeleteMapping("/inspirations/{id}")
    public ApiResult<?> deleteInspiration(@PathVariable Long id) {
        userBizService.deleteInspiration(id);
        return ApiResult.success("OK");
    }

    // ========== 旅程记录 ==========
    @GetMapping("/journeys")
    public ApiResult<?> listJourneys(@RequestParam(defaultValue = "user_001") String userId) {
        List<JourneyPO> journeys = userBizService.listJourneys(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (JourneyPO j : journeys) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("journey", j);
            item.put("points", userBizService.listJourneyPoints(j.getId()));
            item.put("images", userBizService.listJourneyImages(j.getId()));
            result.add(item);
        }
        return ApiResult.success(result);
    }

    @GetMapping("/journeys/{id}")
    public ApiResult<?> getJourney(@PathVariable Long id) {
        JourneyPO j = userBizService.getJourney(id);
        if (j == null) return ApiResult.error("旅程不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("journey", j);
        result.put("points", userBizService.listJourneyPoints(j.getId()));
        result.put("images", userBizService.listJourneyImages(j.getId()));
        return ApiResult.success(result);
    }

    @PostMapping("/journeys")
    public ApiResult<?> addJourney(@RequestBody JourneyPO po) {
        if (po.getUserId() == null) po.setUserId("user_001");
        userBizService.addJourney(po);
        return ApiResult.success(po);
    }

    @PutMapping("/journeys/{id}")
    public ApiResult<?> updateJourney(@PathVariable Long id, @RequestBody JourneyPO po) {
        po.setId(id);
        userBizService.updateJourney(po);
        return ApiResult.success("OK");
    }

    @DeleteMapping("/journeys/{id}")
    public ApiResult<?> deleteJourney(@PathVariable Long id) {
        userBizService.deleteJourney(id);
        return ApiResult.success("OK");
    }

    @PostMapping("/journeys/{id}/points")
    public ApiResult<?> saveJourneyPoints(@PathVariable Long id, @RequestBody List<JourneyPointPO> points) {
        userBizService.saveJourneyPoints(id, points);
        return ApiResult.success("OK");
    }

    @PostMapping("/journeys/{id}/images")
    public ApiResult<?> saveJourneyImages(@PathVariable Long id, @RequestBody List<JourneyImagePO> images) {
        userBizService.saveJourneyImages(id, images);
        return ApiResult.success("OK");
    }

    // ========== 用户偏好 ==========
    @GetMapping("/preferences")
    public ApiResult<?> getPreferences(@RequestParam(defaultValue = "user_001") String userId) {
        UserPreferencePO pref = userBizService.getPreference(userId);
        return ApiResult.success(pref != null ? pref : new UserPreferencePO());
    }

    @PutMapping("/preferences")
    public ApiResult<?> savePreferences(@RequestBody UserPreferencePO po) {
        if (po.getUserId() == null) po.setUserId("user_001");
        userBizService.savePreference(po);
        return ApiResult.success("OK");
    }

    // ========== 用户昵称（从 user_travel_preference.preference_name 读取） ==========
    @GetMapping("/nickname")
    public ApiResult<?> getNickname(@RequestParam(defaultValue = "user_001") String userId) {
        return ApiResult.success(Map.of("nickname", userBizService.getUserNickname(userId)));
    }

    @PutMapping("/nickname")
    public ApiResult<?> updateNickname(@RequestParam(defaultValue = "user_001") String userId,
                                        @RequestBody Map<String, String> body) {
        String nickname = body.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            return ApiResult.error("昵称不能为空");
        }
        userBizService.updateNickname(userId, nickname);
        return ApiResult.success("OK");
    }

    // ========== 头像上传 ==========
    @PostMapping("/avatar")
    public ApiResult<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                    @RequestParam(defaultValue = "user_001") String userId) {
        try {
            String avatarUrl = userBizService.uploadAvatar(file, userId);
            return ApiResult.success(Map.of("avatar", avatarUrl));
        } catch (Exception e) {
            return ApiResult.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/avatar")
    public ApiResult<?> getAvatar(@RequestParam(defaultValue = "user_001") String userId) {
        String avatar = userBizService.getUserProfile(userId);
        return ApiResult.success(Map.of("avatar", avatar != null ? avatar : ""));
    }

    // ========== 地理编码（景点名称 → 经纬度） ==========
    @GetMapping("/geocode")
    public ApiResult<?> geocode(@RequestParam String address) {
        return ApiResult.success(userBizService.geocodeAddress(address));
    }

    // ========== 通用图片上传 ==========
    // 支持灵感目的地、旅程等模块的图片上传
    // category 可选值: inspiration, journey, general
    @PostMapping("/upload")
    public ApiResult<?> uploadImage(@RequestParam("file") MultipartFile file,
                                   @RequestParam(defaultValue = "general") String category) {
        try {
            String imageUrl = userBizService.uploadImage(file, category);
            return ApiResult.success(Map.of("url", imageUrl));
        } catch (IllegalArgumentException e) {
            return ApiResult.error(e.getMessage());
        } catch (Exception e) {
            return ApiResult.error("上传失败: " + e.getMessage());
        }
    }
}
