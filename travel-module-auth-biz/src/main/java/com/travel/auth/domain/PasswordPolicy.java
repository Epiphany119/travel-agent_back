package com.travel.auth.domain;

import com.travel.common.core.exception.BusinessException;

/**
 * 密码策略 - 统一密码强度校验
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    /**
     * 校验密码强度
     * 规则：8-72 位，必须包含大小写字母和数字
     */
    public static void validate(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > 72
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")) {
            throw new BusinessException(400, "密码至少 8 位，且必须包含大小写字母和数字");
        }
    }
}
