package com.travel.mcp.protocol.jsonrpc;

/**
 * JSON-RPC 2.0 错误码常量定义。
 * 
 * <p>遵循 JSON-RPC 2.0 规范定义的错误码范围：</p>
 * <ul>
 *   <li>-32768 ~ -32000：保留用于预定义错误</li>
 *   <li>-32000 以上：可自定义应用错误</li>
 * </ul>
 */
public final class JsonRpcErrorCodes {

    private JsonRpcErrorCodes() {
    }

    /** 解析错误 - 无效的 JSON */
    public static final int PARSE_ERROR = -32700;

    /** 无效请求 - JSON 有效但不是有效的请求对象 */
    public static final int INVALID_REQUEST = -32600;

    /** 方法不存在 */
    public static final int METHOD_NOT_FOUND = -32601;

    /** 无效参数 */
    public static final int INVALID_PARAMS = -32602;

    /** 内部错误 */
    public static final int INTERNAL_ERROR = -32603;

    /** MCP 特定错误：工具未找到 */
    public static final int TOOL_NOT_FOUND = -32001;

    /** MCP 特定错误：工具执行失败 */
    public static final int TOOL_EXECUTION_ERROR = -32002;

    /** MCP 特定错误：会话无效 */
    public static final int INVALID_SESSION = -32003;

    /** MCP 特定错误：流订阅失败 */
    public static final int STREAM_ERROR = -32004;
}
