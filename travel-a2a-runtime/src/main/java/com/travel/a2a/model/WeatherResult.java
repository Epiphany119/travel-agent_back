package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 天气结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResult {

    /**
     * 城市
     */
    @JsonProperty("city")
    private String city;

    /**
     * 是否成功
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * 天气数据（JSON字符串或Map）
     */
    @JsonProperty("data")
    private Object data;

    /**
     * 错误信息
     */
    @JsonProperty("error")
    private String error;
}
