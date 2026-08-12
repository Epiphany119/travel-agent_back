package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 餐饮结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResult {

    /**
     * 餐厅名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 地址
     */
    @JsonProperty("address")
    private String address;

    /**
     * 菜系类型
     */
    @JsonProperty("cuisine")
    private String cuisine;

    /**
     * 人均价格
     */
    @JsonProperty("avgPrice")
    private Integer avgPrice;

    /**
     * 评分
     */
    @JsonProperty("rating")
    private Double rating;
}
