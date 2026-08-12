package com.travel.a2a.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预算估算结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEstimate {

    /**
     * 总预算
     */
    @JsonProperty("totalBudget")
    private double totalBudget;

    /**
     * 每人预算
     */
    @JsonProperty("perPersonBudget")
    private double perPersonBudget;

    /**
     * 是否成功
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * 错误信息
     */
    @JsonProperty("error")
    private String error;

    /**
     * 预算明细
     */
    @JsonProperty("breakdown")
    private List<BudgetItem> breakdown;

    /**
     * 预算明细项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetItem {

        @JsonProperty("category")
        private String category;

        @JsonProperty("amount")
        private double amount;

        @JsonProperty("percent")
        private double percent;

        @JsonProperty("notes")
        private String notes;
    }
}
