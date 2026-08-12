package com.travel.mcp.server.meal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.mcp.protocol.dto.McpServerInfo;
import com.travel.mcp.protocol.dto.McpTool;
import com.travel.mcp.protocol.dto.McpToolResult;
import com.travel.mcp.protocol.jsonrpc.JsonRpcError;
import com.travel.mcp.protocol.jsonrpc.JsonRpcRequest;
import com.travel.mcp.protocol.jsonrpc.JsonRpcResponse;
import com.travel.mcp.server.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MealMcpController {

    private static final String SERVER_NAME = "travel-mcp-server-meal";
    private static final String SERVER_VERSION = "1.0.0";

    private final MealService mealService;
    private final ObjectMapper objectMapper;

    @GetMapping("/mcp/info")
    public Mono<String> getInfo() {
        try {
            McpServerInfo serverInfo = new McpServerInfo(SERVER_NAME, SERVER_VERSION, getToolsList());
            JsonRpcResponse response = JsonRpcResponse.success(1, serverInfo);
            return Mono.just(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to generate server info", e);
            return Mono.just("{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}");
        }
    }

    @PostMapping(value = "/mcp/call", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> call(@RequestBody String body) {
        JsonRpcRequest request = null;
        try {
            log.info("Received MCP call: {}", body);
            request = objectMapper.readValue(body, JsonRpcRequest.class);
            
            String method = request.method();
            Object params = request.params();
            
            Object result;
            if ("tools/call".equals(method) || "tool/call".equals(method)) {
                result = handleToolCall(params);
            } else {
                result = McpToolResult.failure(method, "Unknown method: " + method);
            }
            
            JsonRpcResponse response = JsonRpcResponse.success(request.id(), result);
            return Mono.just(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to process MCP call", e);
            try {
                Object id = request != null ? request.id() : 1;
                JsonRpcResponse errorResponse = JsonRpcResponse.error(id, JsonRpcError.internalError("Internal error: " + e.getMessage()));
                return Mono.just(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception ex) {
                return Mono.just("{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private McpToolResult handleToolCall(Object params) {
        if (params == null) {
            return McpToolResult.failure("unknown", "Missing parameters");
        }

        Map<String, Object> paramMap;
        if (params instanceof Map) {
            paramMap = (Map<String, Object>) params;
        } else {
            return McpToolResult.failure("unknown", "Invalid parameters format");
        }

        String toolName = (String) paramMap.get("name");
        if (toolName == null) {
            return McpToolResult.failure("unknown", "Missing tool name");
        }

        Map<String, Object> arguments = (Map<String, Object>) paramMap.get("arguments");
        if (arguments == null) {
            arguments = Map.of();
        }

        return switch (toolName) {
            case "meal.search" -> executeSearch(arguments);
            default -> McpToolResult.failure(toolName, "Unknown tool: " + toolName);
        };
    }

    private McpToolResult executeSearch(Map<String, Object> arguments) {
        String keywords = (String) arguments.get("keywords");
        String city = (String) arguments.get("city");
        Integer limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : null;

        if (keywords == null || keywords.isBlank()) {
            return McpToolResult.failure("meal.search", "Missing required parameter: keywords");
        }
        if (city == null || city.isBlank()) {
            return McpToolResult.failure("meal.search", "Missing required parameter: city");
        }

        MealService.MealSearchResult result = mealService.search(keywords, city, limit);
        if (result.isSuccess()) {
            return McpToolResult.success("meal.search", result.getData());
        } else {
            return McpToolResult.failure("meal.search", result.getError());
        }
    }

    private List<McpTool> getToolsList() {
        return List.of(
                new McpTool("meal.search", "搜索餐厅/餐饮",
                        Map.of("type", "object", "properties", Map.of(
                                "keywords", Map.of("type", "string", "description", "餐厅关键词，如：火锅、川菜"),
                                "city", Map.of("type", "string", "description", "城市"),
                                "limit", Map.of("type", "number", "description", "返回数量，默认10")
                        ), "required", List.of("keywords", "city")))
        );
    }
}
