package com.travel.module.user.biz.controller;

import com.travel.common.core.result.ApiResult;
import com.travel.module.user.biz.domain.service.UserBizService;
import com.travel.module.user.biz.infra.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin
public class UserDataController {

    private final UserBizService userBizService;

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
}