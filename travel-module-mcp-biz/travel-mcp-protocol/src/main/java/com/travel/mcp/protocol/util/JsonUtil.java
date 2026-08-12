package com.travel.mcp.protocol.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.travel.mcp.protocol.a2a.A2AEvent;
import com.travel.mcp.protocol.a2a.A2AStreamEvent;
import com.travel.mcp.protocol.a2a.A2ATask;
import com.travel.mcp.protocol.dto.McpServerInfo;
import com.travel.mcp.protocol.dto.McpTool;
import com.travel.mcp.protocol.dto.McpToolCall;
import com.travel.mcp.protocol.dto.McpToolResult;
import com.travel.mcp.protocol.jsonrpc.JsonRpcRequest;
import com.travel.mcp.protocol.jsonrpc.JsonRpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * JSON 序列化/反序列化工具类。
 * 
 * <p>封装 Jackson ObjectMapper，提供便捷的 JSON 转换方法。</p>
 */
public class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private JsonUtil() {
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return 配置好的 ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * 安全地将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String safeToJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object", e);
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * 安全地将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @return 反序列化后的对象，失败时返回 null
     */
    public static <T> T safeFromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object", e);
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为复杂类型
     *
     * @param json JSON 字符串
     * @param typeRef 类型引用
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to object", e);
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * 解析 JSON-RPC 请求
     *
     * @param json JSON 字符串
     * @return JsonRpcRequest 对象
     */
    public static JsonRpcRequest parseRequest(String json) {
        return fromJson(json, JsonRpcRequest.class);
    }

    /**
     * 解析 JSON-RPC 响应
     *
     * @param json JSON 字符串
     * @return JsonRpcResponse 对象
     */
    public static JsonRpcResponse parseResponse(String json) {
        return fromJson(json, JsonRpcResponse.class);
    }

    /**
     * 解析 MCP Server 信息
     *
     * @param json JSON 字符串或对象
     * @return McpServerInfo 对象
     */
    public static McpServerInfo parseServerInfo(Object json) {
        if (json instanceof McpServerInfo) {
            return (McpServerInfo) json;
        }
        String jsonStr = json instanceof String ? (String) json : toJson(json);
        return fromJson(jsonStr, McpServerInfo.class);
    }

    /**
     * 解析 A2A 任务
     *
     * @param json JSON 字符串
     * @return A2ATask 对象
     */
    public static A2ATask parseA2ATask(String json) {
        return fromJson(json, A2ATask.class);
    }

    /**
     * 解析 A2A 事件
     *
     * @param json JSON 字符串
     * @return A2AEvent 对象
     */
    public static A2AEvent parseA2AEvent(String json) {
        return fromJson(json, A2AEvent.class);
    }

    /**
     * 解析 A2A SSE 流事件
     *
     * @param json JSON 字符串
     * @return A2AStreamEvent 对象
     */
    public static A2AStreamEvent parseA2AStreamEvent(String json) {
        return fromJson(json, A2AStreamEvent.class);
    }

    /**
     * 解析 MCP 工具调用
     *
     * @param json JSON 字符串
     * @return McpToolCall 对象
     */
    public static McpToolCall parseToolCall(String json) {
        return fromJson(json, McpToolCall.class);
    }

    /**
     * 解析 MCP 工具结果
     *
     * @param json JSON 字符串
     * @return McpToolResult 对象
     */
    public static McpToolResult parseToolResult(String json) {
        return fromJson(json, McpToolResult.class);
    }

    /**
     * 将对象转换为 Map
     *
     * @param obj 要转换的对象
     * @return Map
     */
    public static Map<String, Object> toMap(Object obj) {
        return MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 将 Map 转换为指定类型的对象
     *
     * @param map 源 Map
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        return MAPPER.convertValue(map, clazz);
    }

    /**
     * 将 JSON 字符串解析为 Map
     *
     * @param json JSON 字符串
     * @return Map
     */
    public static Map<String, Object> parseMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 将 JSON 字符串解析为 List
     *
     * @param json JSON 字符串
     * @return List
     */
    public static List<Object> parseList(String json) {
        return fromJson(json, new TypeReference<List<Object>>() {
        });
    }

    /**
     * 将 JSON 字符串解析为 List<Map>
     *
     * @param json JSON 字符串
     * @return List<Map>
     */
    public static List<Map<String, Object>> parseMapList(String json) {
        return fromJson(json, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    /**
     * 创建工具的 inputSchema
     *
     * @param description 参数描述
     * @param properties 属性定义
     * @return inputSchema Map
     */
    public static Map<String, Object> createInputSchema(String description, Map<String, Object> properties) {
        return Map.of(
            "type", "object",
            "description", description,
            "properties", properties
        );
    }

    /**
     * 创建工具属性定义
     *
     * @param type 参数类型
     * @param description 参数描述
     * @return 属性定义 Map
     */
    public static Map<String, Object> createProperty(String type, String description) {
        return createProperty(type, description, null);
    }

    /**
     * 创建工具属性定义（带枚举）
     *
     * @param type 参数类型
     * @param description 参数描述
     * @param enumValues 枚举值
     * @return 属性定义 Map
     */
    public static Map<String, Object> createProperty(String type, String description, List<String> enumValues) {
        if (enumValues != null && !enumValues.isEmpty()) {
            return Map.of(
                "type", type,
                "description", description,
                "enum", enumValues
            );
        }
        return Map.of(
            "type", type,
            "description", description
        );
    }
}
