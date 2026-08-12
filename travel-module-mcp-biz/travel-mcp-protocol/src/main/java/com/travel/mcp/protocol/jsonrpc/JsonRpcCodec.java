package com.travel.mcp.protocol.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * JSON-RPC 编解码工具类。
 * 
 * <p>提供 JSON-RPC 请求和响应的序列化和反序列化功能。</p>
 */
public class JsonRpcCodec {

    private static final Logger log = LoggerFactory.getLogger(JsonRpcCodec.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.findAndRegisterModules();
    }

    private JsonRpcCodec() {
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
     * 将请求对象编码为 JSON 字符串
     *
     * @param req JSON-RPC 请求
     * @return JSON 字符串
     * @throws JsonProcessingException 如果编码失败
     */
    public static String encode(JsonRpcRequest req) throws JsonProcessingException {
        return MAPPER.writeValueAsString(req);
    }

    /**
     * 将响应对象编码为 JSON 字符串
     *
     * @param resp JSON-RPC 响应
     * @return JSON 字符串
     * @throws JsonProcessingException 如果编码失败
     */
    public static String encodeResponse(JsonRpcResponse resp) throws JsonProcessingException {
        return MAPPER.writeValueAsString(resp);
    }

    /**
     * 解码 JSON 字符串为响应对象
     *
     * @param json JSON 字符串
     * @return JsonRpcResponse 对象
     * @throws JsonProcessingException 如果解码失败
     */
    public static JsonRpcResponse decodeResponse(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, JsonRpcResponse.class);
    }

    /**
     * 解码 JSON 字符串为请求对象
     *
     * @param json JSON 字符串
     * @return JsonRpcRequest 对象
     * @throws JsonProcessingException 如果解码失败
     */
    public static JsonRpcRequest decodeRequest(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, JsonRpcRequest.class);
    }

    /**
     * 解析参数字符串为 Map
     *
     * @param json JSON 字符串
     * @return 参数映射
     * @throws JsonProcessingException 如果解析失败
     */
    public static Map<String, Object> parseParams(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 安全地将对象编码为 JSON 字符串
     *
     * @param obj 要编码的对象
     * @return JSON 字符串，失败时返回 null
     */
    public static String safeEncode(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to encode object to JSON", e);
            return null;
        }
    }

    /**
     * 安全地将 JSON 字符串解析为 Map
     *
     * @param json JSON 字符串
     * @return 参数映射，失败时返回 null
     */
    public static Map<String, Object> safeParseParams(String json) {
        try {
            return parseParams(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON params", e);
            return null;
        }
    }
}
