package com.travel.module.user.biz.infra.repository;

import com.travel.module.user.biz.domain.entity.UserTravelPreference;
import com.travel.module.user.biz.domain.repository.UserPreferenceRepository;
import com.travel.module.user.biz.infra.persistence.UserTravelPreferenceMapper;
import com.travel.module.user.biz.infra.persistence.UserTravelPreferencePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户偏好仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserPreferenceRepositoryImpl implements UserPreferenceRepository {
    
    private final UserTravelPreferenceMapper mapper;
    
    @Override
    public UserTravelPreference findByUserIdAndType(String userId, String preferenceType) {
        UserTravelPreferencePO po = mapper.findByUserIdAndType(userId, preferenceType);
        return po != null ? po.toEntity() : null;
    }
    
    @Override
    public List<UserTravelPreference> findByUserId(String userId) {
        List<UserTravelPreferencePO> pos = mapper.findByUserId(userId);
        return pos.stream().map(UserTravelPreferencePO::toEntity).collect(Collectors.toList());
    }
    
    @Override
    public UserTravelPreference save(UserTravelPreference preference) {
        if (preference.getId() == null) {
            // 新增
            preference.setCreatedAt(LocalDateTime.now());
            preference.setUpdatedAt(LocalDateTime.now());
            UserTravelPreferencePO po = UserTravelPreferencePO.fromEntity(preference);
            mapper.insert(po);
            preference.setId(po.getId());
        } else {
            // 更新
            preference.setUpdatedAt(LocalDateTime.now());
            UserTravelPreferencePO po = UserTravelPreferencePO.fromEntity(preference);
            mapper.update(po);
        }
        return preference;
    }
    
    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
}
