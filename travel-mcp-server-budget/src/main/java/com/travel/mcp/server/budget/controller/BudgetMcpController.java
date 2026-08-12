package com.travel.mcp.server.budget.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.mcp.protocol.dto.McpServerInfo;
import com.travel.mcp.protocol.dto.McpTool;
import com.travel.mcp.protocol.dto.McpToolResult;
import com.travel.mcp.protocol.jsonrpc.JsonRpcError;
import com.travel.mcp.protocol.jsonrpc.JsonRpcRequest;
import com.travel.mcp.protocol.jsonrpc.JsonRpcResponse;
import com.travel.mcp.server.budget.service.BudgetService;
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
public class BudgetMcpController {

    private static final String SERVER_NAME = "travel-mcp-server-budget";
    private static final String SERVER_VERSION = "1.0.0";

    private final BudgetService budgetService;
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
            case "budget.estimate" -> executeEstimate(arguments);
            default -> McpToolResult.failure(toolName, "Unknown tool: " + toolName);
        };
    }

    @SuppressWarnings("unchecked")
    private McpToolResult executeEstimate(Map<String, Object> arguments) {
        Object daysObj = arguments.get("days");
        Object citiesObj = arguments.get("cities");
        String level = (String) arguments.get("level");

        if (daysObj == null) {
            return McpToolResult.failure("budget.estimate", "Missing required parameter: days");
        }

        int days;
        if (daysObj instanceof Number) {
            days = ((Number) daysObj).intValue();
        } else {
            try {
                days = Integer.parseInt(daysObj.toString());
            } catch (NumberFormatException e) {
                return McpToolResult.failure("budget.estimate", "Invalid days format");
            }
        }

        List<String> cities;
        if (citiesObj instanceof List) {
            cities = ((List<?>) citiesObj).stream().map(Object::toString).toList();
        } else if (citiesObj instanceof String) {
            cities = List.of((String) citiesObj);
        } else {
            return McpToolResult.failure("budget.estimate", "Missing required parameter: cities");
        }

        BudgetService.BudgetEstimate result = budgetService.estimate(days, cities, level);
        if (result.isSuccess()) {
            return McpToolResult.success("budget.estimate", Map.of(
                    "days", result.getDays(),
                    "cities", result.getCities(),
                    "level", result.getLevel(),
                    "cityCoefficient", result.getCityCoefficient(),
                    "baseCost", result.getCostBreakdown(),
                    "totalCost", result.getTotalCost(),
                    "currency", "CNY",
                    "description", String.format(
                            "根据您的%d天行程，游览%s等城市，选择%s消费水平，预计总费用约%.2f元（仅供参考，实际费用可能因个人习惯而有所不同）。",
                            result.getDays(),
                            String.join("、", result.getCities()),
                            result.getLevel(),
                            result.getTotalCost()
                    )
            ));
        } else {
            return McpToolResult.failure("budget.estimate", result.getError());
        }
    }

    private List<McpTool> getToolsList() {
        return List.of(
                new McpTool("budget.estimate", "估算旅行预算",
                        Map.of("type", "object", "properties", Map.of(
                                "days", Map.of("type", "number", "description", "旅行天数"),
                                "cities", Map.of("type", "array", "items", Map.of("type", "string"), "description", "目的地城市列表"),
                                "level", Map.of("type", "string", "description", "消费水平：economy(经济)/standard(标准)/luxury(豪华)")
                        ), "required", List.of("days", "cities")))
        );
    }
}
