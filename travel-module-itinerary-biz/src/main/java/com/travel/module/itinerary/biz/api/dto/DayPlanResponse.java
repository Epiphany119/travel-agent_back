package com.travel.module.itinerary.biz.api.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class DayPlanResponse {
    private Integer dayNumber;
    private String date;
    private String theme;
    private List<AttractionResponse> attractions;
    private List<MealResponse> meals;
    private String transportation;
    private String notes;
    private Double dayBudget;
}
