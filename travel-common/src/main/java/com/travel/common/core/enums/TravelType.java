package com.travel.common.core.enums;

/**
 * 旅行类型枚举
 */
public enum TravelType {

    SOLO("独自旅行", "solo"),
    COUPLE("情侣出行", "couple"),
    FAMILY("家庭出游", "family"),
    FRIENDS("朋友同行", "friends"),
    BUSINESS("商务出差", "business");

    private final String description;
    private final String code;

    TravelType(String description, String code) {
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
