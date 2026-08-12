package com.travel.mcp.client;

import com.travel.mcp.client.config.McpClientConfig;
import com.travel.mcp.protocol.dto.McpServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * MCP 客户端主类。
 * 
 * <p>管理到所有 MCP Server 的会话，提供便捷的工具调用接口。</p>
 */
@Configuration
public class McpClient {

    private static final Logger log = LoggerFactory.getLogger(McpClient.class);

    private final McpClientConfig config;
    private final McpTransport transport;

    public McpClient(McpClientConfig config, McpTransport transport) {
        this.config = config;
        this.transport = transport;
    }

    /**
     * 天气 MCP Server 会话。
     *
     * @return McpSession 实例
     */
    @Lazy
    @Bean
    public McpSession weatherSession() {
        log.info("Creating weather MCP session: {}", config.getWeatherUrl());
        McpServerInfo info = transport.getServerInfo(config.getWeatherUrl());
        return new McpSession("weather", config.getWeatherUrl(), transport, info);
    }

    /**
     * POI MCP Server 会话。
     *
     * @return McpSession 实例
     */
    @Lazy
    @Bean
    public McpSession poiSession() {
        log.info("Creating POI MCP session: {}", config.getPoiUrl());
        McpServerInfo info = transport.getServerInfo(config.getPoiUrl());
        return new McpSession("poi", config.getPoiUrl(), transport, info);
    }

    /**
     * 餐饮 MCP Server 会话。
     *
     * @return McpSession 实例
     */
    @Lazy
    @Bean
    public McpSession mealSession() {
        log.info("Creating meal MCP session: {}", config.getMealUrl());
        McpServerInfo info = transport.getServerInfo(config.getMealUrl());
        return new McpSession("meal", config.getMealUrl(), transport, info);
    }

    /**
     * 预算 MCP Server 会话。
     *
     * @return McpSession 实例
     */
    @Lazy
    @Bean
    public McpSession budgetSession() {
        log.info("Creating budget MCP session: {}", config.getBudgetUrl());
        McpServerInfo info = transport.getServerInfo(config.getBudgetUrl());
        return new McpSession("budget", config.getBudgetUrl(), transport, info);
    }

    /**
     * 行程规划 MCP Server 会话。
     *
     * @return McpSession 实例
     */
    @Lazy
    @Bean
    public McpSession itinerarySession() {
        log.info("Creating itinerary MCP session: {}", config.getItineraryUrl());
        McpServerInfo info = transport.getServerInfo(config.getItineraryUrl());
        return new McpSession("itinerary", config.getItineraryUrl(), transport, info);
    }

    /**
     * 获取客户端配置。
     *
     * @return McpClientConfig 实例
     */
    public McpClientConfig getConfig() {
        return config;
    }

    /**
     * 获取传输层。
     *
     * @return McpTransport 实例
     */
    public McpTransport getTransport() {
        return transport;
    }
}
