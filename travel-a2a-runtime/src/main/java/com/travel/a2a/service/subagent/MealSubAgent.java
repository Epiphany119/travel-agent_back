package com.travel.a2a.service.subagent;

import com.travel.a2a.model.AgentResult;
import com.travel.a2a.model.MealResult;
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
 * 餐饮子Agent
 */
@Slf4j
@Service
public class MealSubAgent {

    private final McpSession mealSession;

    public MealSubAgent(@Lazy @Qualifier("mealSession") McpSession mealSession) {
        this.mealSession = mealSession;
    }

    /**
     * 搜索餐厅
     *
     * @param request 行程请求
     * @return Agent执行结果
     */
    public AgentResult search(TravelPlanRequest request) {
        long startTime = System.currentTimeMillis();
        String destination = request.getDestination();
        String travelStyle = request.getTravelStyle();

        log.info("MealSubAgent: 搜索餐厅, destination={}, travelStyle={}", destination, travelStyle);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("city", destination);
            params.put("keywords", buildKeywords(travelStyle));
            params.put("offset", 15);

            McpToolResult result = mealSession.callTool("meal.search", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                List<MealResult> meals = parseMealResults(result.result());
                log.info("MealSubAgent: 搜索餐厅成功, count={}, elapsedMs={}", meals.size(), elapsedMs);
                return AgentResult.success("meal", meals, elapsedMs);
            } else {
                log.warn("MealSubAgent: 搜索餐厅失败, error={}", result.error());
                return AgentResult.failure("meal", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("MealSubAgent: 搜索餐厅异常", e);
            return AgentResult.failure("meal", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 搜索指定类型的餐厅
     *
     * @param city   城市
     * @param cuisine 菜系类型
     * @return Agent执行结果
     */
    public AgentResult searchByCuisine(String city, String cuisine) {
        long startTime = System.currentTimeMillis();
        log.info("MealSubAgent: 按菜系搜索餐厅, city={}, cuisine={}", city, cuisine);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("city", city);
            params.put("cuisine", cuisine);
            params.put("offset", 10);

            McpToolResult result = mealSession.callTool("meal.search", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                List<MealResult> meals = parseMealResults(result.result());
                return AgentResult.success("meal", meals, elapsedMs);
            } else {
                return AgentResult.failure("meal", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("MealSubAgent: 按菜系搜索餐厅异常", e);
            return AgentResult.failure("meal", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 获取餐厅详情
     *
     * @param restaurantId 餐厅ID
     * @return Agent执行结果
     */
    public AgentResult getDetail(String restaurantId) {
        long startTime = System.currentTimeMillis();
        log.info("MealSubAgent: 获取餐厅详情, restaurantId={}", restaurantId);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id", restaurantId);

            McpToolResult result = mealSession.callTool("meal.get_detail", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                return AgentResult.success("meal", result.result(), elapsedMs);
            } else {
                return AgentResult.failure("meal", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("MealSubAgent: 获取餐厅详情异常", e);
            return AgentResult.failure("meal", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 构建搜索关键词
     */
    private String buildKeywords(String travelStyle) {
        if (travelStyle == null || travelStyle.isEmpty()) {
            return "特色餐厅";
        }
        return travelStyle + " 美食";
    }

    /**
     * 解析餐饮结果
     */
    @SuppressWarnings("unchecked")
    private List<MealResult> parseMealResults(Object result) {
        List<MealResult> meals = new ArrayList<>();

        if (result instanceof List) {
            List<?> list = (List<?>) result;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    MealResult meal = MealResult.builder()
                            .name(getStringValue(map, "name"))
                            .address(getStringValue(map, "address"))
                            .cuisine(getStringValue(map, "cuisine"))
                            .avgPrice(getIntValue(map, "avgPrice"))
                            .rating(getDoubleValue(map, "rating"))
                            .build();
                    meals.add(meal);
                }
            }
        }

        return meals;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
