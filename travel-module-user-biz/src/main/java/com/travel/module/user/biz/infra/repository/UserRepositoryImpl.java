package com.travel.module.user.biz.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.module.user.biz.domain.entity.User;
import com.travel.module.user.biz.domain.repository.UserRepository;
import com.travel.module.user.biz.infra.persistence.UserMapper;
import com.travel.module.user.biz.infra.persistence.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@Primary
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public User findById(Long userId) {
        UserPO po = userMapper.selectById(userId);
        if (po == null) {
            return null;
        }
        return toUser(po);
    }

    @Override
    public User save(User user) {
        UserPO po = toUserPO(user);
        if (po.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(po);
            user.setId(po.getId());
        } else {
            po.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(po);
        }
        return user;
    }

    private User toUser(UserPO po) {
        User user = new User();
        user.setId(po.getId());
        user.setUsername(po.getUsername());
        user.setNickname(po.getNickname());
        user.setAvatar(po.getAvatar());
        user.setEmail(po.getEmail());
        user.setPhone(po.getPhone());
        user.setFrequentDestination(po.getFrequentDestination());
        user.setDefaultPreferences(po.getDefaultPreferences());
        user.setCreatedAt(po.getCreatedAt());
        user.setUpdatedAt(po.getUpdatedAt());
        return user;
    }

    private UserPO toUserPO(User user) {
        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        po.setNickname(user.getNickname());
        po.setAvatar(user.getAvatar());
        po.setEmail(user.getEmail());
        po.setPhone(user.getPhone());
        po.setFrequentDestination(user.getFrequentDestination());
        po.setDefaultPreferences(user.getDefaultPreferences());
        po.setCreatedAt(user.getCreatedAt());
        po.setUpdatedAt(user.getUpdatedAt());
        return po;
    }
}
