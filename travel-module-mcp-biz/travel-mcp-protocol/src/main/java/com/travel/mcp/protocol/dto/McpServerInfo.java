package com.travel.mcp.protocol.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * MCP Server 信息。
 * 
 * <p>表示一个 MCP Server 的元数据，包含服务器名称、版本和支持的工具列表。</p>
 */
public record McpServerInfo(
    /**
     * 服务器名称
     */
    @JsonProperty("name") String name,

    /**
     * 服务器版本号
     */
    @JsonProperty("version") String version,

    /**
     * 服务器支持的工具列表
     */
    @JsonProperty("tools") List<McpTool> tools
) {
}
