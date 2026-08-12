package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 行程规划请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlanRequest {

    /**
     * 目的地城市
     */
    @JsonProperty("destination")
    private String destination;

    /**
     * 天数
     */
    @JsonProperty("days")
    private int days;

    /**
     * 预算
     */
    @JsonProperty("budget")
    private double budget;

    /**
     * 人数
     */
    @JsonProperty("travelers")
    private int travelers;

    /**
     * 旅行风格
     */
    @JsonProperty("travelStyle")
    private String travelStyle;

    /**
     * 兴趣点
     */
    @JsonProperty("interests")
    private List<String> interests;
}
