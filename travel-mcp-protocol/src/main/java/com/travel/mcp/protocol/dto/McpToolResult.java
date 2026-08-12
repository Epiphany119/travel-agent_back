package com.travel.mcp.protocol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP 工具调用结果。
 * 
 * <p>表示工具调用的执行结果，包含成功/失败状态和返回数据。</p>
 */
public record McpToolResult(
    /**
     * 被调用的工具名称
     */
    @JsonProperty("name") String name,

    /**
     * 调用是否成功
     */
    @JsonProperty("success") boolean success,

    /**
     * 成功时的返回数据
     */
    @JsonProperty("result") Object result,

    /**
     * 失败时的错误信息
     */
    @JsonProperty("error") String error
) {
    /**
     * 创建成功结果
     *
     * @param name 工具名
     * @param result 返回数据
     * @return 成功结果
     */
    public static McpToolResult success(String name, Object result) {
        return new McpToolResult(name, true, result, null);
    }

    /**
     * 创建失败结果
     *
     * @param name 工具名
     * @param error 错误信息
     * @return 失败结果
     */
    public static McpToolResult failure(String name, String error) {
        return new McpToolResult(name, false, null, error);
    }
}
