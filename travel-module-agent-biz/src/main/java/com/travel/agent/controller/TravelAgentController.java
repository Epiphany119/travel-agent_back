package com.travel.agent.controller;

import com.travel.agent.service.TravelPlanningService;
import com.travel.agent.tool.PoiTool;
import com.travel.agent.tool.WeatherTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 旅行规划控制器
 * 提供API接口用于测试和调用
 */
@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelAgentController {

    private final TravelPlanningService travelPlanningService;

    /**
     * 生成旅行行程
     */
    @PostMapping("/plan")
    public Map<String, Object> generateItinerary(@RequestBody Map<String, String> request) {
        String userRequest = request.get("request");
        if (userRequest == null || userRequest.isBlank()) {
            return Map.of("success", false, "message", "请求内容不能为空");
        }

        try {
            String result = travelPlanningService.generateItinerary(userRequest);
            return Map.of("success", true, "data", result);
        } catch (Exception e) {
            log.error("生成行程失败", e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * 查询天气
     */
    @GetMapping("/weather")
    public Map<String, Object> getWeather(@RequestParam String city) {
        try {
            WeatherTool.WeatherResponse response = travelPlanningService.getWeather(city);
            return Map.of("success", response.isSuccess(), "data", response);
        } catch (Exception e) {
            log.error("查询天气失败", e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * 搜索POI
     */
    @GetMapping("/poi")
    public Map<String, Object> searchPOI(
            @RequestParam String keywords,
            @RequestParam(required = false) String city) {
        try {
            PoiTool.AmapResponse response = travelPlanningService.searchPOI(keywords, city);
            return Map.of("success", response.isSuccess(), "data", response);
        } catch (Exception e) {
            log.error("搜索POI失败", e);
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "travel-agent");
    }
}
