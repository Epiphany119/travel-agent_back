package com.travel.a2a.service.subagent;

import com.travel.a2a.model.AgentResult;
import com.travel.a2a.model.WeatherResult;
import com.travel.mcp.client.McpSession;
import com.travel.mcp.protocol.dto.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 天气子Agent
 */
@Slf4j
@Service
public class WeatherSubAgent {

    private final McpSession weatherSession;

    public WeatherSubAgent(@Qualifier("weatherSession") McpSession weatherSession) {
        this.weatherSession = weatherSession;
    }

    /**
     * 获取天气信息
     *
     * @param city 城市名称
     * @return Agent执行结果
     */
    public AgentResult getWeather(String city) {
        long startTime = System.currentTimeMillis();
        log.info("WeatherSubAgent: 获取天气信息, city={}", city);

        try {
            McpToolResult result = weatherSession.callTool("weather.get_forecast",
                    Map.of("city", city, "days", 7));

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                WeatherResult weatherResult = WeatherResult.builder()
                        .city(city)
                        .success(true)
                        .data(result.result())
                        .build();
                log.info("WeatherSubAgent: 获取天气成功, city={}, elapsedMs={}", city, elapsedMs);
                return AgentResult.success("weather", weatherResult, elapsedMs);
            } else {
                log.warn("WeatherSubAgent: 获取天气失败, city={}, error={}", city, result.error());
                return AgentResult.failure("weather", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("WeatherSubAgent: 获取天气异常, city={}", city, e);
            return AgentResult.failure("weather", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 获取当前天气
     *
     * @param city 城市名称
     * @return Agent执行结果
     */
    public AgentResult getCurrentWeather(String city) {
        long startTime = System.currentTimeMillis();
        log.info("WeatherSubAgent: 获取当前天气, city={}", city);

        try {
            McpToolResult result = weatherSession.callTool("weather.get_current",
                    Map.of("city", city));

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                WeatherResult weatherResult = WeatherResult.builder()
                        .city(city)
                        .success(true)
                        .data(result.result())
                        .build();
                return AgentResult.success("weather", weatherResult, elapsedMs);
            } else {
                return AgentResult.failure("weather", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("WeatherSubAgent: 获取当前天气异常, city={}", city, e);
            return AgentResult.failure("weather", e.getMessage(), elapsedMs);
        }
    }
}
