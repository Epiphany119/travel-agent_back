package com.travel.a2a.service.subagent;

import com.travel.a2a.model.AgentResult;
import com.travel.a2a.model.BudgetEstimate;
import com.travel.a2a.model.TravelPlanRequest;
import com.travel.mcp.client.McpSession;
import com.travel.mcp.protocol.dto.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预算子Agent
 */
@Slf4j
@Service
public class BudgetSubAgent {

    private final McpSession budgetSession;

    public BudgetSubAgent(@Lazy @Qualifier("budgetSession") McpSession budgetSession) {
        this.budgetSession = budgetSession;
    }

    /**
     * 估算预算
     *
     * @param request 行程请求
     * @return Agent执行结果
     */
    public AgentResult estimate(TravelPlanRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("BudgetSubAgent: 估算预算, destination={}, days={}, budget={}, travelers={}",
                request.getDestination(), request.getDays(), request.getBudget(), request.getTravelers());

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("destination", request.getDestination());
            params.put("days", request.getDays());
            params.put("budget", request.getBudget());
            params.put("travelers", request.getTravelers());
            params.put("travelStyle", request.getTravelStyle());

            McpToolResult result = budgetSession.callTool("budget.estimate", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                BudgetEstimate estimate = parseBudgetEstimate(result.result(), request);
                log.info("BudgetSubAgent: 估算预算成功, elapsedMs={}", elapsedMs);
                return AgentResult.success("budget", estimate, elapsedMs);
            } else {
                log.warn("BudgetSubAgent: 估算预算失败, error={}", result.error());
                return AgentResult.failure("budget", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("BudgetSubAgent: 估算预算异常", e);
            return AgentResult.failure("budget", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 计算每日预算分配
     *
     * @param totalBudget 总预算
     * @param days        天数
     * @param travelers   人数
     * @return Agent执行结果
     */
    public AgentResult calculateDailyBudget(double totalBudget, int days, int travelers) {
        long startTime = System.currentTimeMillis();
        log.info("BudgetSubAgent: 计算每日预算, totalBudget={}, days={}, travelers={}",
                totalBudget, days, travelers);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("totalBudget", totalBudget);
            params.put("days", days);
            params.put("travelers", travelers);

            McpToolResult result = budgetSession.callTool("budget.calculate", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                return AgentResult.success("budget", result.result(), elapsedMs);
            } else {
                return AgentResult.failure("budget", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("BudgetSubAgent: 计算每日预算异常", e);
            return AgentResult.failure("budget", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 解析预算估算结果
     */
    @SuppressWarnings("unchecked")
    private BudgetEstimate parseBudgetEstimate(Object result, TravelPlanRequest request) {
        BudgetEstimate.BudgetEstimateBuilder builder = BudgetEstimate.builder()
                .totalBudget(request.getBudget())
                .perPersonBudget(request.getBudget() / request.getTravelers())
                .success(true)
                .breakdown(new ArrayList<>());

        if (result instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) result;
            builder.totalBudget(getDoubleValue(map, "totalBudget", request.getBudget()));
            builder.perPersonBudget(getDoubleValue(map, "perPersonBudget",
                    request.getBudget() / request.getTravelers()));
            builder.error(getStringValue(map, "error"));

            // 解析预算明细
            Object breakdownObj = map.get("breakdown");
            if (breakdownObj instanceof List) {
                List<BudgetEstimate.BudgetItem> breakdown = new ArrayList<>();
                for (Object item : (List<?>) breakdownObj) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        BudgetEstimate.BudgetItem budgetItem = BudgetEstimate.BudgetItem.builder()
                                .category(getStringValue(itemMap, "category"))
                                .amount(getDoubleValue(itemMap, "amount", 0))
                                .percent(getDoubleValue(itemMap, "percent", 0))
                                .notes(getStringValue(itemMap, "notes"))
                                .build();
                        breakdown.add(budgetItem);
                    }
                }
                builder.breakdown(breakdown);
            }
        }

        return builder.build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}
