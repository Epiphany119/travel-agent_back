package com.travel.mcp.protocol.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 错误对象。
 * 
 * <p>当 JSON-RPC 请求处理失败时，返回此错误对象。</p>
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
public record JsonRpcError(
    /**
     * 错误码，负整数
     */
    @JsonProperty("code") int code,

    /**
     * 错误消息，简短描述
     */
    @JsonProperty("message") String message,

    /**
     * 错误附加数据，可选
     */
    @JsonProperty("data") Object data
) {
    /**
     * 创建解析错误
     */
    public static JsonRpcError parseError(String message) {
        return new JsonRpcError(-32700, message, null);
    }

    /**
     * 创建无效请求错误
     */
    public static JsonRpcError invalidRequest(String message) {
        return new JsonRpcError(-32600, message, null);
    }

    /**
     * 创建方法未找到错误
     */
    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(-32601, "Method not found: " + method, null);
    }

    /**
     * 创建无效参数错误
     */
    public static JsonRpcError invalidParams(String message) {
        return new JsonRpcError(-32602, message, null);
    }

    /**
     * 创建内部错误
     */
    public static JsonRpcError internalError(String message) {
        return new JsonRpcError(-32603, message, null);
    }
}
