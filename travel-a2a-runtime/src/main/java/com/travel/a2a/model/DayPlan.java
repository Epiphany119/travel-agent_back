package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 每日行程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayPlan {

    /**
     * 第几天
     */
    @JsonProperty("day")
    private int day;

    /**
     * 日期
     */
    @JsonProperty("date")
    private String date;

    /**
     * 天气
     */
    @JsonProperty("weather")
    private String weather;

    /**
     * 温度
     */
    @JsonProperty("temperature")
    private String temperature;

    /**
     * 行程安排（按时间顺序的活动）
     */
    @JsonProperty("activities")
    private List<Activity> activities;

    /**
     * 当日预算
     */
    @JsonProperty("dailyBudget")
    private double dailyBudget;

    /**
     * 活动
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activity {

        /**
         * 时间，如 "09:00"
         */
        @JsonProperty("time")
        private String time;

        /**
         * 活动类型：sightseeing/meal/shopping/transport/rest
         */
        @JsonProperty("type")
        private String type;

        /**
         * 活动名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 地点
         */
        @JsonProperty("location")
        private String location;

        /**
         * 预计时长（分钟）
         */
        @JsonProperty("duration")
        private int duration;

        /**
         * 备注
         */
        @JsonProperty("notes")
        private String notes;

        /**
         * 费用
         */
        @JsonProperty("cost")
        private double cost;
    }
}
