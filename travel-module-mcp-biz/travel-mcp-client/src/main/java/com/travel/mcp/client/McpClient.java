package com.travel.mcp.client;

import com.travel.mcp.client.config.McpClientConfig;
import com.travel.mcp.protocol.dto.McpServerInfo;
import com.travel.mcp.protocol.dto.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

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
        return new McpSession("weather", config.getWeatherUrl(), transport, resolveServerInfo(config.getWeatherUrl()));
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
        return new McpSession("poi", config.getPoiUrl(), transport, resolveServerInfo(config.getPoiUrl()));
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
        return new McpSession("meal", config.getMealUrl(), transport, resolveServerInfo(config.getMealUrl()));
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
        return new McpSession("budget", config.getBudgetUrl(), transport, resolveServerInfo(config.getBudgetUrl()));
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
        return new McpSession("itinerary", config.getItineraryUrl(), transport, resolveServerInfo(config.getItineraryUrl()));
    }

    /**
     * 安全地获取 MCP Server 信息。
     *
     * <p>当目标 MCP Server 未启动或不可达时，返回一个空的降级信息而非抛出异常，
     * 从而保证应用即使在没有完整 MCP 环境时也能正常启动。工具调用阶段的失败
     * 由调用方（如各个 SubAgent）自行处理。</p>
     *
     * @param serverUrl MCP Server URL
     * @return 服务器信息（连接失败时返回降级信息）
     */
    private McpServerInfo resolveServerInfo(String serverUrl) {
        try {
            return transport.getServerInfo(serverUrl);
        } catch (Exception e) {
            log.warn("MCP server 不可达，使用降级信息: url={}, error={}", serverUrl, e.getMessage());
            return new McpServerInfo("unknown", "0.0.0", List.of());
        }
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
