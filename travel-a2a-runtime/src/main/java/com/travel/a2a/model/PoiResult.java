package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POI结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoiResult {

    /**
     * 名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 地址
     */
    @JsonProperty("address")
    private String address;

    /**
     * 类型
     */
    @JsonProperty("type")
    private String type;

    /**
     * 距离（米）
     */
    @JsonProperty("distance")
    private Integer distance;

    /**
     * 电话
     */
    @JsonProperty("tel")
    private String tel;

    /**
     * 经度
     */
    @JsonProperty("longitude")
    private Double longitude;

    /**
     * 纬度
     */
    @JsonProperty("latitude")
    private Double latitude;
}
