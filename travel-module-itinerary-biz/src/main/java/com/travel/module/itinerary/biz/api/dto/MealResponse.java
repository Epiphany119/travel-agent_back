package com.travel.module.itinerary.biz.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class MealResponse {
    private String mealType;
    private String restaurantName;
    private String cuisine;
    private Double avgPrice;
    private String address;
    private String reason;
    private Double rating;
}
