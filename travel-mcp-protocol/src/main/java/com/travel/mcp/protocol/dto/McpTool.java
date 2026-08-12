package com.travel.mcp.protocol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MCP 工具定义。
 * 
 * <p>表示一个可调用的 MCP 工具，包含工具名称、描述和输入参数模式。</p>
 */
public record McpTool(
    /**
     * 工具唯一标识，格式为 "namespace.method"，如 "weather.get_forecast"
     */
    @JsonProperty("name") String name,

    /**
     * 工具功能描述，用于 AI 模型理解工具用途
     */
    @JsonProperty("description") String description,

    /**
     * 输入参数 JSON Schema 定义
     */
    @JsonProperty("inputSchema") Map<String, Object> inputSchema
) {
}
