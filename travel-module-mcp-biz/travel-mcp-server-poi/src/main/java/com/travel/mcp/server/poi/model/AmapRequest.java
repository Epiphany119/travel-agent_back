package com.travel.mcp.server.poi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmapRequest {
    private String action;
    private String address;
    private String city;
    private String location;
    private String extensions;
    private String keywords;
    private String types;
    private Boolean citylimit;
    private Integer offset;
    private Integer page;
    private String datatype;
    private String origin;
    private String destination;
    private String waypoints;
    private String province;
    private String number;
    private Integer nightflag;
    private Integer strategy;
    private String origins;
    private Integer type;
}
