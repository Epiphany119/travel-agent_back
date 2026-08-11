package com.travel.module.itinerary.biz.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Input contract for a safe, structured travel-plan generation request. */
@Data
public class GenerateItineraryRequest {
    @NotBlank(message = "请输入目的地")
    private String destination;
    @NotNull @Min(value = 1, message = "至少安排 1 天") @Max(value = 14, message = "单次最多安排 14 天")
    private Integer days;
    @NotNull @Min(value = 300, message = "预算至少为 300 元") @Max(value = 200000, message = "预算超出单次规划范围")
    private Double budget;
    @Min(1) @Max(12)
    private Integer travelers = 1;
    private String travelStyle = "深度体验";
    private List<String> interests;
}
