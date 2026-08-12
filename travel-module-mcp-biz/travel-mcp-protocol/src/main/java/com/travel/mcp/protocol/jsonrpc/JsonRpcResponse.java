package com.travel.mcp.protocol.jsonrpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 响应对象。
 * 
 * <p>表示一个 JSON-RPC 2.0 规范的响应，包含结果或错误信息。</p>
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonRpcResponse(
    /**
     * JSON-RPC 版本标识，固定为 "2.0"
     */
    @JsonProperty("jsonrpc") String jsonrpc,

    /**
     * 对应请求的标识符
     */
    @JsonProperty("id") Object id,

    /**
     * 请求成功时的结果，与 error 字段互斥
     */
    @JsonProperty("result") Object result,

    /**
     * 请求失败时的错误信息，与 result 字段互斥
     */
    @JsonProperty("error") JsonRpcError error
) {
    /**
     * 判断响应是否成功（无错误）
     *
     * @return true 如果成功，false 如果包含错误
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * 获取结果或抛出异常。
     *
     * @return 请求结果
     * @throws McpProtocolException 如果响应包含错误
     */
    public Object getResultOrThrow() {
        if (error != null) {
            throw new McpProtocolException(error.message(), error.code());
        }
        return result;
    }

    /**
     * 创建成功响应
     */
    public static JsonRpcResponse success(Object id, Object result) {
        return new JsonRpcResponse("2.0", id, result, null);
    }

    /**
     * 创建错误响应
     */
    public static JsonRpcResponse error(Object id, JsonRpcError error) {
        return new JsonRpcResponse("2.0", id, null, error);
    }
}
