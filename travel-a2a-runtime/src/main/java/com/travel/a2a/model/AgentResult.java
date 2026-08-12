package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent执行结果（内部使用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /**
     * 来源：weather/poi/meal/budget
     */
    @JsonProperty("source")
    private String source;

    /**
     * 是否成功
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * 结果数据
     */
    @JsonProperty("data")
    private Object data;

    /**
     * 错误信息
     */
    @JsonProperty("error")
    private String error;

    /**
     * 耗时（毫秒）
     */
    @JsonProperty("elapsedMs")
    private long elapsedMs;

    /**
     * 创建成功结果
     */
    public static AgentResult success(String source, Object data, long elapsedMs) {
        return AgentResult.builder()
                .source(source)
                .success(true)
                .data(data)
                .elapsedMs(elapsedMs)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static AgentResult failure(String source, String error, long elapsedMs) {
        return AgentResult.builder()
                .source(source)
                .success(false)
                .error(error)
                .elapsedMs(elapsedMs)
                .build();
    }
}
