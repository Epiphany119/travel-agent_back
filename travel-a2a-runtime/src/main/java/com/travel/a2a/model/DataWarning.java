package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据警告（用于降级场景）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataWarning {

    /**
     * 来源：weather/poi/meal/budget
     */
    @JsonProperty("source")
    private String source;

    /**
     * 警告信息
     */
    @JsonProperty("message")
    private String message;

    /**
     * 耗时（毫秒）
     */
    @JsonProperty("elapsedMs")
    private long elapsedMs;
}
