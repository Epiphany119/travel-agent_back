package com.travel.mcp.protocol.a2a;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * A2A (Agent-to-Agent) 任务定义。
 * 
 * <p>表示一个跨 Agent 的任务请求，包含任务标识、发送方、接收方、输入数据和流式标记。</p>
 */
public record A2ATask(
    /**
     * 任务唯一标识
     */
    @JsonProperty("taskId") String taskId,

    /**
     * 任务发送方 Agent ID
     */
    @JsonProperty("from") String from,

    /**
     * 任务接收方 Agent ID
     */
    @JsonProperty("to") String to,

    /**
     * 任务输入数据
     */
    @JsonProperty("input") Map<String, Object> input,

    /**
     * 是否启用流式响应
     */
    @JsonProperty("stream") boolean stream
) {
    /**
     * 创建非流式的 A2A 任务
     *
     * @param taskId 任务ID
     * @param from 发送方
     * @param to 接收方
     * @param input 输入数据
     * @return 新的 A2ATask 实例
     */
    public static A2ATask of(String taskId, String from, String to, Map<String, Object> input) {
        return new A2ATask(taskId, from, to, input, false);
    }

    /**
     * 创建流式的 A2A 任务
     *
     * @param taskId 任务ID
     * @param from 发送方
     * @param to 接收方
     * @param input 输入数据
     * @return 新的 A2ATask 实例
     */
    public static A2ATask streaming(String taskId, String from, String to, Map<String, Object> input) {
        return new A2ATask(taskId, from, to, input, true);
    }
}
