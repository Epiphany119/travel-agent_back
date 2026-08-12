package com.travel.mcp.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP Client 配置类。
 * 
 * <p>管理各 MCP Server 的 URL 配置，支持通过环境变量覆盖。</p>
 */
@Component
@ConfigurationProperties(prefix = "travel.mcp")
public class McpClientConfig {

    /**
     * 天气 MCP Server URL
     */
    private String weatherUrl = "http://localhost:8081";

    /**
     * POI MCP Server URL
     */
    private String poiUrl = "http://localhost:8082";

    /**
     * 餐饮 MCP Server URL
     */
    private String mealUrl = "http://localhost:8083";

    /**
     * 预算 MCP Server URL
     */
    private String budgetUrl = "http://localhost:8084";

    /**
     * 行程规划 MCP Server URL
     */
    private String itineraryUrl = "http://localhost:8085";

    public String getWeatherUrl() {
        return weatherUrl;
    }

    public void setWeatherUrl(String weatherUrl) {
        this.weatherUrl = weatherUrl;
    }

    public String getPoiUrl() {
        return poiUrl;
    }

    public void setPoiUrl(String poiUrl) {
        this.poiUrl = poiUrl;
    }

    public String getMealUrl() {
        return mealUrl;
    }

    public void setMealUrl(String mealUrl) {
        this.mealUrl = mealUrl;
    }

    public String getBudgetUrl() {
        return budgetUrl;
    }

    public void setBudgetUrl(String budgetUrl) {
        this.budgetUrl = budgetUrl;
    }

    public String getItineraryUrl() {
        return itineraryUrl;
    }

    public void setItineraryUrl(String itineraryUrl) {
        this.itineraryUrl = itineraryUrl;
    }
}
