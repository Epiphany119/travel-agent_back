package com.travel.mcp.protocol.jsonrpc;

/**
 * MCP 协议异常。
 * 
 * <p>当 MCP 协议处理过程中发生错误时抛出此异常。</p>
 */
public class McpProtocolException extends RuntimeException {

    private final int code;

    public McpProtocolException(String message, int code) {
        super(message);
        this.code = code;
    }

    public McpProtocolException(String message, int code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isParseError() {
        return code == JsonRpcErrorCodes.PARSE_ERROR;
    }

    public boolean isInvalidRequest() {
        return code == JsonRpcErrorCodes.INVALID_REQUEST;
    }

    public boolean isMethodNotFound() {
        return code == JsonRpcErrorCodes.METHOD_NOT_FOUND;
    }

    public boolean isInvalidParams() {
        return code == JsonRpcErrorCodes.INVALID_PARAMS;
    }

    public boolean isInternalError() {
        return code == JsonRpcErrorCodes.INTERNAL_ERROR;
    }
}
