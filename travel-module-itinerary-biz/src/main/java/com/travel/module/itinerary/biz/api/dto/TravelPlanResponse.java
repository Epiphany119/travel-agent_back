package com.travel.module.itinerary.biz.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class TravelPlanResponse {
    private String planId;
    private String destination;
    private Integer days;
    private Double totalBudget;
    private Double estimatedCost;
    private String budgetStatus;
    private String overview;
    private List<DayPlanResponse> dayPlans;
    private List<String> travelTips;
    private List<String> packingList;
    /** Providers that supplied facts in this response; used by the UI to disclose data provenance. */
    private List<String> dataSources;
    /** A provider failure is explicit instead of being silently replaced with made-up recommendations. */
    private List<String> dataWarnings;
}
