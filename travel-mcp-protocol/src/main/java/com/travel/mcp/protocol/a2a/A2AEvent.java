package com.travel.mcp.protocol.a2a;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A2A (Agent-to-Agent) 事件定义。
 * 
 * <p>表示 A2A 通信中的事件，包含事件类型和事件数据。</p>
 */
public record A2AEvent(
    /**
     * 事件类型
     */
    @JsonProperty("type") String type,

    /**
     * 事件数据
     */
    @JsonProperty("data") Object data
) {
    /**
     * 事件类型常量：文本 token
     */
    public static final String TYPE_TOKEN = "token";

    /**
     * 事件类型常量：工具调用
     */
    public static final String TYPE_TOOL_CALL = "tool_call";

    /**
     * 事件类型常量：工具调用结果
     */
    public static final String TYPE_TOOL_RESULT = "tool_result";

    /**
     * 事件类型常量：进度更新
     */
    public static final String TYPE_PROGRESS = "progress";

    /**
     * 事件类型常量：最终结果
     */
    public static final String TYPE_FINAL = "final";

    /**
     * 事件类型常量：错误
     */
    public static final String TYPE_ERROR = "error";

    /**
     * 创建 token 事件
     *
     * @param token 文本 token
     * @return 新的 A2AEvent 实例
     */
    public static A2AEvent token(String token) {
        return new A2AEvent(TYPE_TOKEN, token);
    }

    /**
     * 创建工具调用事件
     *
     * @param toolCall 工具调用信息
     * @return 新的 A2AEvent 实例
     */
    public static A2AEvent toolCall(Object toolCall) {
        return new A2AEvent(TYPE_TOOL_CALL, toolCall);
    }

    /**
     * 创建工具结果事件
     *
     * @param toolResult 工具执行结果
     * @return 新的 A2AEvent 实例
     */
    public static A2AEvent toolResult(Object toolResult) {
        return new A2AEvent(TYPE_TOOL_RESULT, toolResult);
    }

    /**
     * 创建进度事件
     *
     * @param progress 进度信息
     * @return 新的 A2AEvent 实例
     */
    public static A2AEvent progress(Object progress) {
        return new A2AEvent(TYPE_PROGRESS, progress);
    }

    /**
     * 创建最终结果事件
     *
     * @param result 最终结果
     * @return 新的 A2AEvent 实例
     */
    public static A2AEvent finalResult(Object result) {
        return new A2AEvent(TYPE_FINAL, result);
    }

    /**
     * 创建错误事件
     *
     * @param error 错误信息
     * @return 新的 A2AEvent 实例
     */
    public static A2AEvent error(Object error) {
        return new A2AEvent(TYPE_ERROR, error);
    }

    /**
     * 判断是否为 token 事件
     */
    public boolean isToken() {
        return TYPE_TOKEN.equals(type);
    }

    /**
     * 判断是否为工具调用事件
     */
    public boolean isToolCall() {
        return TYPE_TOOL_CALL.equals(type);
    }

    /**
     * 判断是否为工具结果事件
     */
    public boolean isToolResult() {
        return TYPE_TOOL_RESULT.equals(type);
    }

    /**
     * 判断是否为最终结果事件
     */
    public boolean isFinal() {
        return TYPE_FINAL.equals(type);
    }

    /**
     * 判断是否为错误事件
     */
    public boolean isError() {
        return TYPE_ERROR.equals(type);
    }
}
