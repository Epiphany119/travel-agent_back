package com.travel.mcp.protocol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MCP 工具调用请求。
 * 
 * <p>表示一次工具调用的请求，包含工具名和参数。</p>
 */
public record McpToolCall(
    /**
     * 要调用的工具名称，格式为 "namespace.method"
     */
    @JsonProperty("name") String name,

    /**
     * 工具调用参数，键值对形式
     */
    @JsonProperty("arguments") Map<String, Object> arguments
) {
    /**
     * 创建工具调用请求的便捷工厂方法
     *
     * @param name 工具名
     * @param arguments 参数映射
     * @return 新的 McpToolCall 实例
     */
    public static McpToolCall of(String name, Map<String, Object> arguments) {
        return new McpToolCall(name, arguments);
    }
}
