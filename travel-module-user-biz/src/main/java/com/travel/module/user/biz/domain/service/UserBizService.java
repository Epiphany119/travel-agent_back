package com.travel.module.user.biz.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.module.user.biz.infra.persistence.*;
import com.travel.module.user.biz.infra.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBizService {

    private final InspirationMapper inspirationMapper;
    private final JourneyMapper journeyMapper;
    private final JourneyPointMapper journeyPointMapper;
    private final JourneyImageMapper journeyImageMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserTravelPreferenceMapper userTravelPreferenceMapper;
    private final ImageStorageService imageStorageService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${travel.amap.api-key:}")
    private String amapApiKey;

    // ========== 地理编码（景点名称 → 经纬度） ==========
    public Map<String, BigDecimal> geocodeAddress(String address) {
        if (amapApiKey == null || amapApiKey.isBlank()) {
            // API Key 未配置时返回空结果
            return Map.of();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                    .queryParam("key", amapApiKey)
                    .queryParam("address", address)
                    .build()
                    .toUriString();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return Map.of();
            String status = String.valueOf(response.get("status"));
            if (!"1".equals(status)) return Map.of();
            var geocodes = (java.util.List<Map<String, Object>>) response.get("geocodes");
            if (geocodes == null || geocodes.isEmpty()) return Map.of();
            String location = (String) geocodes.get(0).get("location");
            if (location == null || location.isBlank()) return Map.of();
            String[] parts = location.split(",");
            if (parts.length != 2) return Map.of();
            BigDecimal lng = new BigDecimal(parts[0].trim());
            BigDecimal lat = new BigDecimal(parts[1].trim());
            return Map.of("longitude", lng, "latitude", lat);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ========== 用户偏好 ==========
    public UserPreferencePO getPreference(String userId) {
        LambdaQueryWrapper<UserPreferencePO> w = new LambdaQueryWrapper<>();
        w.eq(UserPreferencePO::getUserId, userId);
        w.eq(UserPreferencePO::getPreferenceType, "default");
        UserPreferencePO existing = userPreferenceMapper.selectOne(w);
        if (existing == null) {
            // 首次访问自动创建一条默认记录，保证 nickname 有持久化位置
            existing = new UserPreferencePO();
            existing.setUserId(userId);
            existing.setPreferenceType("default");
            existing.setName("旅人");
            userPreferenceMapper.insert(existing);
        }
        return existing;
    }

    @Transactional
    public void savePreference(UserPreferencePO po) {
        UserPreferencePO existing = getPreference(po.getUserId());
        if (existing != null) {
            po.setId(existing.getId());
            po.setCreatedAt(existing.getCreatedAt());
            userPreferenceMapper.updateById(po);
        } else {
            po.setPreferenceType("default");
            userPreferenceMapper.insert(po);
        }
    }

    // ========== 灵感目的地 ==========
    public List<InspirationPO> listInspirations(String userId) {
        LambdaQueryWrapper<InspirationPO> w = new LambdaQueryWrapper<>();
        w.eq(InspirationPO::getUserId, userId);
        w.orderByDesc(InspirationPO::getSortOrder).orderByDesc(InspirationPO::getCreatedAt);
        return inspirationMapper.selectList(w);
    }

    @Transactional
    public InspirationPO addInspiration(InspirationPO po) {
        if (po.getName() == null) po.setName("");
        if (po.getImageUrl() == null) po.setImageUrl("");
        if (po.getQuote() == null) po.setQuote("");
        if (po.getBestSeason() == null) po.setBestSeason("");
        if (po.getStatus() == null) po.setStatus(1);
        if (po.getPriority() == null) po.setPriority(0);
        if (po.getEstimatedBudget() == null) po.setEstimatedBudget(0);
        if (po.getSortOrder() == null) po.setSortOrder(0);
        inspirationMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateInspiration(InspirationPO po) {
        inspirationMapper.updateById(po);
    }

    @Transactional
    public void deleteInspiration(Long id) {
        inspirationMapper.deleteById(id);
    }

    // ========== 旅程记录 ==========
    public List<JourneyPO> listJourneys(String userId) {
        LambdaQueryWrapper<JourneyPO> w = new LambdaQueryWrapper<>();
        w.eq(JourneyPO::getUserId, userId);
        w.orderByDesc(JourneyPO::getStartDate);
        return journeyMapper.selectList(w);
    }

    public JourneyPO getJourney(Long id) {
        return journeyMapper.selectById(id);
    }

    @Transactional
    public JourneyPO addJourney(JourneyPO po) {
        // 为必填字段设置默认值，避免 NOT NULL 报错
        if (po.getSummary() == null) po.setSummary("");
        if (po.getTravelType() == null) po.setTravelType("");
        if (po.getCompanions() == null) po.setCompanions("");
        if (po.getWeatherInfo() == null) po.setWeatherInfo("");
        if (po.getStatus() == null) po.setStatus(1);
        if (po.getStartDate() == null) po.setStartDate(LocalDate.now());
        if (po.getEndDate() == null) po.setEndDate(LocalDate.now());
        if (po.getTotalDays() == null) po.setTotalDays(0);
        if (po.getTotalCost() == null) po.setTotalCost(0);
        if (po.getRating() == null) po.setRating(0);
        journeyMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateJourney(JourneyPO po) {
        journeyMapper.updateById(po);
    }

    @Transactional
    public void deleteJourney(Long id) {
        journeyPointMapper.delete(new LambdaQueryWrapper<JourneyPointPO>().eq(JourneyPointPO::getJourneyId, id));
        journeyImageMapper.delete(new LambdaQueryWrapper<JourneyImagePO>().eq(JourneyImagePO::getJourneyId, id));
        journeyMapper.deleteById(id);
    }

    // ========== 途经地点 ==========
    public List<JourneyPointPO> listJourneyPoints(Long journeyId) {
        LambdaQueryWrapper<JourneyPointPO> w = new LambdaQueryWrapper<>();
        w.eq(JourneyPointPO::getJourneyId, journeyId);
        w.orderByAsc(JourneyPointPO::getSortOrder);
        return journeyPointMapper.selectList(w);
    }

    @Transactional
    public void saveJourneyPoints(Long journeyId, List<JourneyPointPO> points) {
        journeyPointMapper.delete(new LambdaQueryWrapper<JourneyPointPO>().eq(JourneyPointPO::getJourneyId, journeyId));
        for (JourneyPointPO p : points) {
            p.setJourneyId(journeyId);
            // 为必填字段设置默认值，避免 NOT NULL 报错
            if (p.getLatitude() == null) p.setLatitude(java.math.BigDecimal.ZERO);
            if (p.getLongitude() == null) p.setLongitude(java.math.BigDecimal.ZERO);
            if (p.getVisitDate() == null) p.setVisitDate(LocalDate.now());
            if (p.getDescription() == null) p.setDescription("");
            if (p.getSortOrder() == null) p.setSortOrder(0);
            journeyPointMapper.insert(p);
        }
    }

    // ========== 旅程照片 ==========
    public List<JourneyImagePO> listJourneyImages(Long journeyId) {
        LambdaQueryWrapper<JourneyImagePO> w = new LambdaQueryWrapper<>();
        w.eq(JourneyImagePO::getJourneyId, journeyId);
        w.orderByAsc(JourneyImagePO::getSortOrder);
        return journeyImageMapper.selectList(w);
    }

    @Transactional
    public void saveJourneyImages(Long journeyId, List<JourneyImagePO> images) {
        journeyImageMapper.delete(new LambdaQueryWrapper<JourneyImagePO>().eq(JourneyImagePO::getJourneyId, journeyId));
        for (JourneyImagePO img : images) {
            img.setJourneyId(journeyId);
            journeyImageMapper.insert(img);
        }
    }

    // ========== 用户头像 ==========
    public String getUserProfile(String userId) {
        UserProfilePO profile = userProfileMapper.findByUserId(userId);
        return profile != null ? profile.getAvatar() : null;
    }

    public String uploadAvatar(MultipartFile file, String userId) throws Exception {
        // 保存文件到用户家目录下的 travel-agent-uploads 目录
        String fileName = file.getOriginalFilename();
        String ext = fileName != null && fileName.contains(".") 
            ? fileName.substring(fileName.lastIndexOf(".")) 
            : ".jpg";
        String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        
        String uploadDir = System.getProperty("user.home") + "/travel-agent-uploads/avatar/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File dest = new File(uploadDir + newFileName);
        file.transferTo(dest);
        
        // 生成访问URL
        String avatarUrl = "/uploads/avatar/" + newFileName;
        
        // 更新数据库
        UserProfilePO profile = new UserProfilePO();
        profile.setUserId(userId);
        profile.setUsername(userId);
        profile.setAvatar(avatarUrl);
        profile.setAvatarUrl(avatarUrl);
        saveUserProfile(profile);
        
        return avatarUrl;
    }

    /**
     * 通用图片上传
     * @param file 上传的文件
     * @param category 分类目录: inspiration, journey, general
     * @return 图片访问 URL
     */
    public String uploadImage(MultipartFile file, String category) {
        return imageStorageService.store(file, category);
    }

    public void saveUserProfile(UserProfilePO po) {
        UserProfilePO existing = userProfileMapper.findByUserId(po.getUserId());
        if (existing != null) {
            po.setId(existing.getId());
            po.setCreatedAt(existing.getCreatedAt());
            userProfileMapper.updateByUserId(po);
        } else {
            userProfileMapper.insert(po);
        }
    }

    // ========== 用户昵称（从 user_travel_preference.name 读取） ==========
    public String getUserNickname(String userId) {
        UserTravelPreferencePO pref = userTravelPreferenceMapper.findByUserIdAndType(userId, "default");
        if (pref != null && pref.getName() != null && !pref.getName().isBlank()) {
            return pref.getName();
        }
        return "旅人";
    }

    /**
     * 更新 user_travel_preference.name（用于侧边栏昵称持久化）
     */
    public void updateNickname(String userId, String nickname) {
        UserTravelPreferencePO pref = userTravelPreferenceMapper.findByUserIdAndType(userId, "default");
        if (pref == null) {
            pref = new UserTravelPreferencePO();
            pref.setUserId(userId);
            pref.setPreferenceType("default");
            pref.setName(nickname);
            userTravelPreferenceMapper.insert(pref);
        } else {
            pref.setName(nickname);
            userTravelPreferenceMapper.update(pref);
        }
    }
}