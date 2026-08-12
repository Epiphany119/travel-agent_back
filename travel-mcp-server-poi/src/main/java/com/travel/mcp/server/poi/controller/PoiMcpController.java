package com.travel.mcp.server.poi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.mcp.protocol.dto.McpServerInfo;
import com.travel.mcp.protocol.dto.McpTool;
import com.travel.mcp.protocol.dto.McpToolResult;
import com.travel.mcp.protocol.jsonrpc.JsonRpcError;
import com.travel.mcp.protocol.jsonrpc.JsonRpcRequest;
import com.travel.mcp.protocol.jsonrpc.JsonRpcResponse;
import com.travel.mcp.server.poi.model.AmapResponse;
import com.travel.mcp.server.poi.service.DistanceService;
import com.travel.mcp.server.poi.service.GeocodeService;
import com.travel.mcp.server.poi.service.PoiSearchService;
import com.travel.mcp.server.poi.service.RouteService;
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
public class PoiMcpController {

    private static final String SERVER_NAME = "travel-mcp-server-poi";
    private static final String SERVER_VERSION = "1.0.0";

    private final GeocodeService geocodeService;
    private final PoiSearchService poiSearchService;
    private final RouteService routeService;
    private final DistanceService distanceService;
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
            case "poi.geocode" -> executeGeocode(arguments);
            case "poi.regeo" -> executeRegeo(arguments);
            case "poi.search" -> executeSearch(arguments);
            case "poi.inputtips" -> executeInputtips(arguments);
            case "poi.route_walking" -> executeRouteWalking(arguments);
            case "poi.route_transit" -> executeRouteTransit(arguments);
            case "poi.route_driving" -> executeRouteDriving(arguments);
            case "poi.route_bicycling" -> executeRouteBicycling(arguments);
            case "poi.distance" -> executeDistance(arguments);
            default -> McpToolResult.failure(toolName, "Unknown tool: " + toolName);
        };
    }

    private McpToolResult executeGeocode(Map<String, Object> arguments) {
        String address = (String) arguments.get("address");
        String city = (String) arguments.get("city");
        if (address == null || address.isBlank()) {
            return McpToolResult.failure("poi.geocode", "Missing required parameter: address");
        }
        AmapResponse response = geocodeService.geocode(address, city);
        return toToolResult("poi.geocode", response);
    }

    private McpToolResult executeRegeo(Map<String, Object> arguments) {
        String location = (String) arguments.get("location");
        if (location == null || location.isBlank()) {
            return McpToolResult.failure("poi.regeo", "Missing required parameter: location");
        }
        AmapResponse response = geocodeService.regeo(location, null);
        return toToolResult("poi.regeo", response);
    }

    private McpToolResult executeSearch(Map<String, Object> arguments) {
        String keywords = (String) arguments.get("keywords");
        String city = (String) arguments.get("city");
        String types = (String) arguments.get("types");
        Integer limit = arguments.get("limit") != null ? ((Number) arguments.get("limit")).intValue() : null;
        if (keywords == null || keywords.isBlank()) {
            return McpToolResult.failure("poi.search", "Missing required parameter: keywords");
        }
        if (city == null || city.isBlank()) {
            return McpToolResult.failure("poi.search", "Missing required parameter: city");
        }
        AmapResponse response = poiSearchService.search(keywords, city, types, limit);
        return toToolResult("poi.search", response);
    }

    private McpToolResult executeInputtips(Map<String, Object> arguments) {
        String keywords = (String) arguments.get("keywords");
        String city = (String) arguments.get("city");
        if (keywords == null || keywords.isBlank()) {
            return McpToolResult.failure("poi.inputtips", "Missing required parameter: keywords");
        }
        AmapResponse response = poiSearchService.inputtips(keywords, city, null);
        return toToolResult("poi.inputtips", response);
    }

    private McpToolResult executeRouteWalking(Map<String, Object> arguments) {
        String origin = (String) arguments.get("origin");
        String destination = (String) arguments.get("destination");
        if (origin == null || origin.isBlank()) {
            return McpToolResult.failure("poi.route_walking", "Missing required parameter: origin");
        }
        if (destination == null || destination.isBlank()) {
            return McpToolResult.failure("poi.route_walking", "Missing required parameter: destination");
        }
        AmapResponse response = routeService.walking(origin, destination);
        return toToolResult("poi.route_walking", response);
    }

    private McpToolResult executeRouteTransit(Map<String, Object> arguments) {
        String origin = (String) arguments.get("origin");
        String destination = (String) arguments.get("destination");
        String city = (String) arguments.get("city");
        if (origin == null || origin.isBlank()) {
            return McpToolResult.failure("poi.route_transit", "Missing required parameter: origin");
        }
        if (destination == null || destination.isBlank()) {
            return McpToolResult.failure("poi.route_transit", "Missing required parameter: destination");
        }
        if (city == null || city.isBlank()) {
            return McpToolResult.failure("poi.route_transit", "Missing required parameter: city");
        }
        AmapResponse response = routeService.transit(origin, destination, city, null);
        return toToolResult("poi.route_transit", response);
    }

    private McpToolResult executeRouteDriving(Map<String, Object> arguments) {
        String origin = (String) arguments.get("origin");
        String destination = (String) arguments.get("destination");
        String waypoints = (String) arguments.get("waypoints");
        if (origin == null || origin.isBlank()) {
            return McpToolResult.failure("poi.route_driving", "Missing required parameter: origin");
        }
        if (destination == null || destination.isBlank()) {
            return McpToolResult.failure("poi.route_driving", "Missing required parameter: destination");
        }
        AmapResponse response = routeService.driving(origin, destination, waypoints, null, null);
        return toToolResult("poi.route_driving", response);
    }

    private McpToolResult executeRouteBicycling(Map<String, Object> arguments) {
        String origin = (String) arguments.get("origin");
        String destination = (String) arguments.get("destination");
        if (origin == null || origin.isBlank()) {
            return McpToolResult.failure("poi.route_bicycling", "Missing required parameter: origin");
        }
        if (destination == null || destination.isBlank()) {
            return McpToolResult.failure("poi.route_bicycling", "Missing required parameter: destination");
        }
        AmapResponse response = routeService.bicycling(origin, destination);
        return toToolResult("poi.route_bicycling", response);
    }

    private McpToolResult executeDistance(Map<String, Object> arguments) {
        Object originsObj = arguments.get("origins");
        String destination = (String) arguments.get("destination");
        if (originsObj == null) {
            return McpToolResult.failure("poi.distance", "Missing required parameter: origins");
        }
        if (destination == null || destination.isBlank()) {
            return McpToolResult.failure("poi.distance", "Missing required parameter: destination");
        }
        String origins;
        if (originsObj instanceof List) {
            origins = String.join(";", ((List<?>) originsObj).stream().map(Object::toString).toList());
        } else {
            origins = originsObj.toString();
        }
        AmapResponse response = distanceService.distance(origins, destination, null);
        return toToolResult("poi.distance", response);
    }

    private McpToolResult toToolResult(String toolName, AmapResponse response) {
        if (response.isSuccess()) {
            return McpToolResult.success(toolName, response);
        } else {
            return McpToolResult.failure(toolName, response.getMessage());
        }
    }

    private List<McpTool> getToolsList() {
        return List.of(
                new McpTool("poi.geocode", "地址转坐标",
                        Map.of("type", "object", "properties", Map.of(
                                "address", Map.of("type", "string", "description", "地址"),
                                "city", Map.of("type", "string", "description", "城市")
                        ), "required", List.of("address"))),
                new McpTool("poi.regeo", "坐标转地址",
                        Map.of("type", "object", "properties", Map.of(
                                "location", Map.of("type", "string", "description", "坐标，经度,纬度")
                        ), "required", List.of("location"))),
                new McpTool("poi.search", "POI搜索",
                        Map.of("type", "object", "properties", Map.of(
                                "keywords", Map.of("type", "string", "description", "关键词"),
                                "city", Map.of("type", "string", "description", "城市"),
                                "types", Map.of("type", "string", "description", "类型"),
                                "limit", Map.of("type", "number", "description", "返回数量")
                        ), "required", List.of("keywords", "city"))),
                new McpTool("poi.inputtips", "输入提示",
                        Map.of("type", "object", "properties", Map.of(
                                "keywords", Map.of("type", "string", "description", "关键词"),
                                "city", Map.of("type", "string", "description", "城市")
                        ), "required", List.of("keywords"))),
                new McpTool("poi.route_walking", "步行路线",
                        Map.of("type", "object", "properties", Map.of(
                                "origin", Map.of("type", "string", "description", "起点坐标"),
                                "destination", Map.of("type", "string", "description", "终点坐标")
                        ), "required", List.of("origin", "destination"))),
                new McpTool("poi.route_transit", "公交路线",
                        Map.of("type", "object", "properties", Map.of(
                                "origin", Map.of("type", "string", "description", "起点坐标"),
                                "destination", Map.of("type", "string", "description", "终点坐标"),
                                "city", Map.of("type", "string", "description", "城市")
                        ), "required", List.of("origin", "destination", "city"))),
                new McpTool("poi.route_driving", "驾车路线",
                        Map.of("type", "object", "properties", Map.of(
                                "origin", Map.of("type", "string", "description", "起点坐标"),
                                "destination", Map.of("type", "string", "description", "终点坐标"),
                                "waypoints", Map.of("type", "string", "description", "途经点")
                        ), "required", List.of("origin", "destination"))),
                new McpTool("poi.route_bicycling", "骑行路线",
                        Map.of("type", "object", "properties", Map.of(
                                "origin", Map.of("type", "string", "description", "起点坐标"),
                                "destination", Map.of("type", "string", "description", "终点坐标")
                        ), "required", List.of("origin", "destination"))),
                new McpTool("poi.distance", "距离测量",
                        Map.of("type", "object", "properties", Map.of(
                                "origins", Map.of("type", "array", "items", Map.of("type", "string"), "description", "起点坐标列表"),
                                "destination", Map.of("type", "string", "description", "终点坐标")
                        ), "required", List.of("origins", "destination")))
        );
    }
}
