package com.travel.mcp.protocol.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * JSON-RPC 2.0 请求对象。
 * 
 * <p>表示一个 JSON-RPC 2.0 规范的请求，包含方法名、参数和请求ID。</p>
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
public record JsonRpcRequest(
    /**
     * JSON-RPC 版本标识，固定为 "2.0"
     */
    @JsonProperty("jsonrpc") String jsonrpc,

    /**
     * 请求的唯一标识符，可以是字符串、数字或null。
     * 用于匹配响应与请求。
     */
    @JsonProperty("id") Object id,

    /**
     * 要调用的方法名，格式为 "namespace.method"
     */
    @JsonProperty("method") String method,

    /**
     * 方法参数，键值对形式
     */
    @JsonProperty("params") Map<String, Object> params
) {
    /**
     * 快速创建一个 JSON-RPC 请求的工厂方法。
     *
     * @param id 请求标识
     * @param method 方法名
     * @param params 参数映射
     * @return 新的 JsonRpcRequest 实例
     */
    public static JsonRpcRequest call(Object id, String method, Map<String, Object> params) {
        return new JsonRpcRequest("2.0", id, method, params);
    }
}
