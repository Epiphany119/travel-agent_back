package com.travel.agent.controller;

import com.travel.common.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 熔断器状态控制器
 */
@RestController
@RequestMapping("/api/ratelimit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    /**
     * 获取熔断器状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();

        // 天气服务状态
        Map<String, Object> weatherStatus = new HashMap<>();
        weatherStatus.put("currentCount", rateLimitService.getCurrentCount("weather"));
        weatherStatus.put("limit", rateLimitService.getLimit("weather"));
        weatherStatus.put("remaining", rateLimitService.getRemaining("weather"));
        result.put("weather", weatherStatus);

        // 高德地图服务状态
        Map<String, Object> amapStatus = new HashMap<>();
        amapStatus.put("currentCount", rateLimitService.getCurrentCount("amap"));
        amapStatus.put("limit", rateLimitService.getLimit("amap"));
        amapStatus.put("remaining", rateLimitService.getRemaining("amap"));
        result.put("amap", amapStatus);

        return result;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "ratelimit");
    }
}
