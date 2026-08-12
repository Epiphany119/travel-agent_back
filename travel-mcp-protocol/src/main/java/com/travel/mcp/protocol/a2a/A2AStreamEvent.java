package com.travel.mcp.protocol.a2a;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A2A SSE 流事件封装。
 * 
 * <p>用于 SSE (Server-Sent Events) 传输协议，封装事件名称和事件数据。</p>
 */
public record A2AStreamEvent(
    /**
     * SSE 事件名称，对应 SSE 的 "event:" 字段
     */
    @JsonProperty("event") String event,

    /**
     * 事件数据，通常是 A2AEvent 的 JSON 序列化结果
     */
    @JsonProperty("data") Object data
) {
    /**
     * SSE 事件名称：心跳
     */
    public static final String EVENT_HEARTBEAT = "heartbeat";

    /**
     * SSE 事件名称：任务更新
     */
    public static final String EVENT_TASK_UPDATE = "task_update";

    /**
     * SSE 事件名称：流式 token
     */
    public static final String EVENT_TOKEN = "token";

    /**
     * SSE 事件名称：工具调用
     */
    public static final String EVENT_TOOL_CALL = "tool_call";

    /**
     * SSE 事件名称：工具结果
     */
    public static final String EVENT_TOOL_RESULT = "tool_result";

    /**
     * SSE 事件名称：任务完成
     */
    public static final String EVENT_TASK_DONE = "task_done";

    /**
     * SSE 事件名称：错误
     */
    public static final String EVENT_ERROR = "error";

    /**
     * 创建心跳事件
     *
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent heartbeat() {
        return new A2AStreamEvent(EVENT_HEARTBEAT, System.currentTimeMillis());
    }

    /**
     * 创建任务更新事件
     *
     * @param data 任务更新数据
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent taskUpdate(Object data) {
        return new A2AStreamEvent(EVENT_TASK_UPDATE, data);
    }

    /**
     * 创建 token 事件
     *
     * @param token 文本 token
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent token(Object token) {
        return new A2AStreamEvent(EVENT_TOKEN, token);
    }

    /**
     * 创建工具调用事件
     *
     * @param toolCall 工具调用信息
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent toolCall(Object toolCall) {
        return new A2AStreamEvent(EVENT_TOOL_CALL, toolCall);
    }

    /**
     * 创建工具结果事件
     *
     * @param toolResult 工具执行结果
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent toolResult(Object toolResult) {
        return new A2AStreamEvent(EVENT_TOOL_RESULT, toolResult);
    }

    /**
     * 创建任务完成事件
     *
     * @param result 最终结果
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent taskDone(Object result) {
        return new A2AStreamEvent(EVENT_TASK_DONE, result);
    }

    /**
     * 创建错误事件
     *
     * @param error 错误信息
     * @return 新的 A2AStreamEvent 实例
     */
    public static A2AStreamEvent error(Object error) {
        return new A2AStreamEvent(EVENT_ERROR, error);
    }
}
