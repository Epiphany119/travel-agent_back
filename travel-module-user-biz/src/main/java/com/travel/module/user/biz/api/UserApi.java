package com.travel.module.user.biz.api;

import com.travel.common.core.result.ApiResult;
import com.travel.module.user.biz.domain.service.UserBizService;
import com.travel.module.user.biz.infra.persistence.InspirationPO;
import com.travel.module.user.biz.infra.persistence.JourneyPO;
import com.travel.module.user.biz.infra.persistence.JourneyPointPO;
import com.travel.module.user.biz.infra.persistence.JourneyImagePO;
import com.travel.module.user.biz.infra.persistence.UserPreferencePO;
import com.travel.module.user.biz.infra.persistence.TravelNotePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserApi {

    private final UserBizService userBizService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/users/search")
    public ApiResult<?> searchUsers(@RequestParam String q) { return ApiResult.success(userBizService.searchUsers(q)); }
    @GetMapping("/social/notes")
    public ApiResult<?> publicNotes(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ApiResult.success(userBizService.listPublicNotes(page, size)); }
    @GetMapping("/social/notes/{id}")
    public ApiResult<?> publicNote(@PathVariable Long id) { return ApiResult.success(userBizService.getPublicNote(id)); }
    @PostMapping("/social/notes/{id}/reaction")
    public ApiResult<?> react(@PathVariable Long id, @RequestParam String type, @RequestParam(defaultValue="user_001") String userId) { userBizService.reactNote(id,userId,type); return ApiResult.success("OK"); }
    @GetMapping("/social/notes/{id}/comments")
    public ApiResult<?> comments(@PathVariable Long id) { return ApiResult.success(userBizService.listComments(id)); }
    @PostMapping("/social/notes/{id}/comments")
    public ApiResult<?> comment(@PathVariable Long id, @RequestParam(defaultValue="user_001") String userId, @RequestBody Map<String,String> body) { userBizService.addComment(id,userId,body.getOrDefault("content","")); return ApiResult.success("OK"); }
    @PostMapping("/users/{id}/friend-request")
    public ApiResult<?> friend(@PathVariable String id, @RequestParam(defaultValue="user_001") String from, @RequestBody(required=false) Map<String,String> body) { userBizService.requestFriend(from,id,body == null ? "" : body.getOrDefault("message","")); return ApiResult.success("OK"); }
    @PostMapping("/social/notes")
    public ApiResult<?> publishSocialNote(@RequestBody Map<String,Object> body) { return ApiResult.success(userBizService.publishSocialNote(body)); }

    @GetMapping("/travel-notes")
    public ApiResult<?> listTravelNotes(@RequestParam(defaultValue = "user_001") String userId) { return ApiResult.success(userBizService.listTravelNotes(userId)); }

    @GetMapping("/travel-notes/{id}")
    public ApiResult<?> getTravelNote(@PathVariable Long id) { return ApiResult.success(userBizService.getTravelNote(id)); }

    @PostMapping("/travel-notes")
    public ApiResult<?> saveTravelNote(@RequestBody TravelNotePO note) { return ApiResult.success(userBizService.saveTravelNote(note)); }

    @PostMapping("/travel-notes/{id}/copy")
    public ApiResult<?> copyTravelNote(@PathVariable Long id, @RequestParam(defaultValue = "user_001") String userId) { return ApiResult.success(userBizService.copyTravelNote(id, userId)); }

    @GetMapping("/travel-notes/share/{token}")
    public ApiResult<?> getSharedTravelNote(@PathVariable String token) { return ApiResult.success(userBizService.getSharedTravelNote(token)); }

    @DeleteMapping("/travel-notes/{id}")
    public ApiResult<?> deleteTravelNote(@PathVariable Long id) { userBizService.deleteTravelNote(id); return ApiResult.success("OK"); }

    @GetMapping("/test")
    public ApiResult<?> test() {
        return ApiResult.success(Map.of("message", "OK"));
    }

    // ========== 用户偏好（合并 auth_account 邮箱信息） ==========
    @GetMapping("/preferences")
    public ApiResult<?> getPreferences(@RequestParam(defaultValue = "user_001") String userId) {
        UserPreferencePO pref = userBizService.getPreference(userId);

        // 查询 auth_account 表获取邮箱和用户名
        Map<String, Object> authInfo = getUserAuthInfo(userId);

        // 构造返回结果（Map 形式，包含偏好数据 + 认证信息）
        Map<String, Object> result = new LinkedHashMap<>();
        if (pref != null) {
            // 反射式拷贝 UserPreferencePO 字段到 Map
            result.put("id", pref.getId());
            result.put("userId", pref.getUserId());
            result.put("preferenceType", pref.getPreferenceType());
            result.put("preferenceName", pref.getPreferenceName());
            result.put("favoriteDestinations", pref.getFavoriteDestinations());
            result.put("preferredSeason", pref.getPreferredSeason());
            result.put("budgetLevel", pref.getBudgetLevel());
            result.put("dailyBudgetMin", pref.getDailyBudgetMin());
            result.put("dailyBudgetMax", pref.getDailyBudgetMax());
            result.put("travelStyle", pref.getTravelStyle());
            result.put("interests", pref.getInterests());
            result.put("dietaryRequirements", pref.getDietaryRequirements());
            result.put("preferredCuisines", pref.getPreferredCuisines());
            result.put("accommodationType", pref.getAccommodationType());
            result.put("accommodationRequirements", pref.getAccommodationRequirements());
            result.put("transportationPreference", pref.getTransportationPreference());
            result.put("travelCompanion", pref.getTravelCompanion());
            result.put("hasChildren", pref.getHasChildren());
            result.put("childrenAges", pref.getChildrenAges());
            result.put("activityLevel", pref.getActivityLevel());
            result.put("pacePreference", pref.getPacePreference());
            result.put("mobilityRequirements", pref.getMobilityRequirements());
            result.put("specialRequests", pref.getSpecialRequests());
        }

        // 合并 auth 信息
        result.put("email", authInfo.get("email"));
        result.put("username", authInfo.get("username"));
        result.put("name", authInfo.get("username"));

        return ApiResult.success(result);
    }

    /**
     * 根据 userId 从 auth_account 表查询邮箱和用户名
     */
    private Map<String, Object> getUserAuthInfo(String userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("email", null);
        result.put("username", null);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT email, username FROM auth_account WHERE user_id = ? LIMIT 1",
                    userId);
            if (!rows.isEmpty()) {
                result.put("email", rows.get(0).get("email"));
                result.put("username", rows.get(0).get("username"));
                log.debug("查询用户认证信息: userId={}, email={}, username={}",
                        userId, rows.get(0).get("email"), rows.get(0).get("username"));
            }
        } catch (Exception e) {
            log.warn("查询用户认证信息失败: userId={}, error={}", userId, e.getMessage());
        }
        return result;
    }

    @PutMapping("/preferences")
    public ApiResult<?> savePreferences(@RequestBody UserPreferencePO po) {
        if (po.getUserId() == null) po.setUserId("user_001");
        userBizService.savePreference(po);
        return ApiResult.success("OK");
    }

    // ========== 用户昵称 ==========
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

    // ========== 地理编码 ==========
    @GetMapping("/geocode")
    public ApiResult<?> geocode(@RequestParam String address) {
        try {
            // 简单的占位实现
            Map<String, Object> result = new HashMap<>();
            result.put("latitude", 0.0);
            result.put("longitude", 0.0);
            result.put("address", address);
            return ApiResult.success(result);
        } catch (Exception e) {
            return ApiResult.error("地理编码失败: " + e.getMessage());
        }
    }

    // ========== 通用图片上传 ==========
    @PostMapping("/upload")
    public ApiResult<?> uploadImage(@RequestParam("file") MultipartFile file,
                                     @RequestParam(defaultValue = "general") String category) {
        try {
            String url = userBizService.uploadAvatar(file, "temp");
            return ApiResult.success(Map.of("url", url));
        } catch (Exception e) {
            return ApiResult.error("上传失败: " + e.getMessage());
        }
    }
}
