package com.travel.common.core.enums;

/**
 * 会话状态枚举
 */
public enum SessionStatus {

    ACTIVE("进行中", "active"),
    COMPLETED("已完成", "completed"),
    EXPIRED("已过期", "expired");

    private final String description;
    private final String code;

    SessionStatus(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}
