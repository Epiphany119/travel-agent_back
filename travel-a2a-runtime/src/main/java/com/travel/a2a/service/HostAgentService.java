package com.travel.a2a.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.a2a.model.*;
import com.travel.a2a.service.orchestrator.ItineraryOrchestrator;
import com.travel.mcp.protocol.a2a.A2AStreamEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 主Agent服务（协调者）
 * 
 * <p>负责协调子Agent执行、收集结果、LLM优化，并通过SSE流输出各阶段事件。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostAgentService {

    private static final long SSE_TIMEOUT = 300_000L; // 5分钟

    private final ItineraryOrchestrator orchestrator;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * 执行行程规划并通过SSE流输出
     *
     * @param request 行程请求
     * @param taskId  任务ID
     * @return SseEmitter
     */
    @Async("taskExecutor")
    public void plan(TravelPlanRequest request, String taskId, SseEmitter emitter) {
        // 设置完成和超时回调
        emitter.onCompletion(() -> log.info("SSE流完成: taskId={}", taskId));
        emitter.onTimeout(() -> log.warn("SSE流超时: taskId={}", taskId));
        emitter.onError(e -> log.error("SSE流异常: taskId={}", taskId, e));

        try {
            executePlan(request, taskId, emitter);
        } catch (Exception e) {
            log.error("执行行程规划异常: taskId={}", taskId, e);
            sendError(emitter, e.getMessage());
        } finally {
            emitter.complete();
        }
    }

    /**
     * 执行行程规划
     */
    private void executePlan(TravelPlanRequest request, String taskId, SseEmitter emitter) {
        try {
            // 1. 发送任务开始事件
            sendEvent(emitter, "task_update", A2AStreamEvent.taskUpdate(
                    java.util.Map.of("taskId", taskId, "status", "started",
                            "message", "开始规划行程...")));

            // 2. 发送工具调用事件（并行调用前）
            sendEvent(emitter, "tool_call", A2AStreamEvent.toolCall(
                    java.util.Map.of("source", "weather", "action", "获取天气信息")));

            sendEvent(emitter, "tool_call", A2AStreamEvent.toolCall(
                    java.util.Map.of("source", "poi", "action", "搜索景点")));

            sendEvent(emitter, "tool_call", A2AStreamEvent.toolCall(
                    java.util.Map.of("source", "meal", "action", "搜索餐厅")));

            sendEvent(emitter, "tool_call", A2AStreamEvent.toolCall(
                    java.util.Map.of("source", "budget", "action", "估算预算")));

            // 3. 并行执行子Agent编排
            TravelPlanResult result = orchestrator.orchestrate(request);

            // 4. 发送工具结果事件
            for (DataWarning warning : result.getDataWarnings()) {
                sendEvent(emitter, "tool_result", A2AStreamEvent.toolResult(
                        java.util.Map.of("source", warning.getSource(),
                                "type", "warning",
                                "message", warning.getMessage(),
                                "elapsedMs", warning.getElapsedMs())));
            }

            // 发送成功的结果
            if (result.getWeather() != null && result.getWeather().isSuccess()) {
                sendEvent(emitter, "tool_result", A2AStreamEvent.toolResult(
                        java.util.Map.of("source", "weather", "type", "success")));
            }
            if (result.getPois() != null && !result.getPois().isEmpty()) {
                sendEvent(emitter, "tool_result", A2AStreamEvent.toolResult(
                        java.util.Map.of("source", "poi", "type", "success",
                                "count", result.getPois().size())));
            }
            if (result.getMeals() != null && !result.getMeals().isEmpty()) {
                sendEvent(emitter, "tool_result", A2AStreamEvent.toolResult(
                        java.util.Map.of("source", "meal", "type", "success",
                                "count", result.getMeals().size())));
            }
            if (result.getBudget() != null && result.getBudget().isSuccess()) {
                sendEvent(emitter, "tool_result", A2AStreamEvent.toolResult(
                        java.util.Map.of("source", "budget", "type", "success")));
            }

            // 5. 发送LLM优化中的token
            sendEvent(emitter, "task_update", A2AStreamEvent.taskUpdate(
                    java.util.Map.of("taskId", taskId, "status", "optimizing",
                            "message", "LLM优化行程中...")));

            // 6. 使用LLM优化行程
            String finalPlan = optimizeWithLLM(result);

            // 分段发送最终行程
            if (finalPlan != null && !finalPlan.isEmpty()) {
                sendEvent(emitter, "token", A2AStreamEvent.token(finalPlan));
            } else {
                // 如果LLM优化失败，发送原始行程数据
                String resultJson = objectMapper.writeValueAsString(result);
                sendEvent(emitter, "token", A2AStreamEvent.token(resultJson));
            }

            // 7. 发送完成事件
            result.setFinalPlan(finalPlan);
            sendEvent(emitter, "task_done", A2AStreamEvent.taskDone(result));

            log.info("行程规划完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("执行行程规划失败: taskId={}", taskId, e);
            sendError(emitter, e.getMessage());
        }
    }

    /**
     * 使用LLM优化行程
     */
    private String optimizeWithLLM(TravelPlanResult result) {
        log.info("HostAgentService: 开始LLM优化");

        try {
            String systemPrompt = """
                    你是一个专业的旅行规划助手，擅长根据收集到的旅行数据生成精美的行程规划。

                    请根据以下数据，为用户生成一份详细的行程规划：

                    1. 根据天气数据，给出穿着建议和出行提示
                    2. 根据景点信息，安排合理的游览顺序
                    3. 根据餐厅信息，推荐每日的早中晚餐
                    4. 根据预算分配，给出消费建议

                    请用markdown格式输出，包含：
                    - 每日行程概览
                    - 详细时间安排
                    - 推荐景点及简介
                    - 餐饮推荐
                    - 穿着和注意事项

                    语气要友好、专业，行程要切实可行。
                    """;

            StringBuilder userMessage = new StringBuilder();
            userMessage.append("目的地：").append(result.getWeather() != null ?
                    result.getWeather().getCity() : "未知").append("\n\n");

            if (result.getDayPlans() != null && !result.getDayPlans().isEmpty()) {
                userMessage.append("行程安排：\n");
                for (DayPlan dayPlan : result.getDayPlans()) {
                    userMessage.append("\n## 第").append(dayPlan.getDay()).append("天 (")
                            .append(dayPlan.getDate()).append(")\n");
                    userMessage.append("天气：").append(dayPlan.getWeather() != null ?
                            dayPlan.getWeather() : "未知").append("\n");

                    if (dayPlan.getActivities() != null) {
                        for (DayPlan.Activity activity : dayPlan.getActivities()) {
                            userMessage.append("- ").append(activity.getTime())
                                    .append(" ").append(activity.getName());
                            if (activity.getLocation() != null) {
                                userMessage.append(" @ ").append(activity.getLocation());
                            }
                            userMessage.append("\n");
                        }
                    }
                }
            }

            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(userMessage.toString()));
            Prompt prompt = new Prompt(messages);

            ChatResponse response = chatModel.call(prompt);

            if (response != null && response.getResult() != null
                    && response.getResult().getOutput() != null) {
                String content = response.getResult().getOutput().getContent();
                log.info("HostAgentService: LLM优化完成, length={}", content.length());
                return content;
            }

        } catch (Exception e) {
            log.error("HostAgentService: LLM优化异常", e);
        }

        return null;
    }

    /**
     * 发送SSE事件
     */
    private void sendEvent(SseEmitter emitter, String eventName, A2AStreamEvent event) {
        try {
            String data = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("发送SSE事件失败: eventName={}", eventName, e);
        }
    }

    /**
     * 发送错误事件
     */
    private void sendError(SseEmitter emitter, String errorMessage) {
        try {
            sendEvent(emitter, "error", A2AStreamEvent.error(
                    java.util.Map.of("message", errorMessage)));
        } catch (Exception e) {
            log.warn("发送错误事件失败", e);
        }
    }
}
