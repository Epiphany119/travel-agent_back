package com.travel.module.itinerary.biz.domain.entity;

import lombok.Data;

/**
 * 景点参观记录
 */
@Data
public class AttractionVisit {

    /** 景点名称 */
    private String name;
    /** 景点描述 */
    private String description;
    /** 预计游览时长（小时） */
    private Double duration;
    /** 门票价格 */
    private Double ticketPrice;
    /** 开放时间 */
    private String openTime;
    /** 地址 */
    private String address;
    /** 评分 */
    private Double rating;
    /** 建议游览顺序 */
    private Integer visitOrder;
}
