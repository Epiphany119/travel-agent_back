package com.travel.module.user.biz.domain.repository;

import com.travel.module.user.biz.domain.entity.User;

public interface UserRepository {

    User findById(Long userId);

    User save(User user);
}
