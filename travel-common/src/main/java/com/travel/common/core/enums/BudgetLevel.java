package com.travel.common.core.enums;

/**
 * 预算等级枚举
 */
public enum BudgetLevel {

    ECONOMY("穷游", "economy"),
    STANDARD("平价", "standard"),
    LUXURY("轻奢", "luxury");

    private final String description;
    private final String code;

    BudgetLevel(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public static BudgetLevel fromCode(String code) {
        for (BudgetLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return STANDARD;
    }
}
