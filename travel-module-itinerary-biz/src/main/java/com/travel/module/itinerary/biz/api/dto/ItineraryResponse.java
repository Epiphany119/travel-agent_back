package com.travel.module.itinerary.biz.api.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class ItineraryResponse {
    private String itineraryId;
    private String sessionId;
    private String destination;
    private String startDate;
    private String endDate;
    private Integer days;
    private Double totalBudget;
    private List<DayPlanResponse> dayPlans;
    private String createdAt;
}
