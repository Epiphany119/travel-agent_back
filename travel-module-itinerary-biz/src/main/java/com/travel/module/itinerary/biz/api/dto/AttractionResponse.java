package com.travel.module.itinerary.biz.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class AttractionResponse {
    private String name;
    private String description;
    private Double duration;
    private Double ticketPrice;
    private String openTime;
    private String address;
    private Double rating;
    private Integer visitOrder;
}
