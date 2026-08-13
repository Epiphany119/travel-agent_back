package com.travel.module.user.biz.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.module.user.biz.infra.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
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

    // ========== 用户偏好 ==========
    public UserPreferencePO getPreference(String userId) {
        LambdaQueryWrapper<UserPreferencePO> w = new LambdaQueryWrapper<>();
        w.eq(UserPreferencePO::getUserId, userId);
        w.eq(UserPreferencePO::getPreferenceType, "default");
        return userPreferenceMapper.selectOne(w);
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
        // 保存文件到 uploads/avatar 目录
        String fileName = file.getOriginalFilename();
        String ext = fileName != null && fileName.contains(".") 
            ? fileName.substring(fileName.lastIndexOf(".")) 
            : ".jpg";
        String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatar/";
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
}