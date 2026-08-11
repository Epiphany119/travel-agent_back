package com.travel.module.agent.biz.domain.service;

import com.travel.module.agent.biz.domain.entity.ChatMessage;
import com.travel.module.agent.biz.domain.entity.ChatSession;
import com.travel.module.agent.biz.domain.entity.TravelPreference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent核心服务 - 实现ReAct模式的推理和执行
 */
@Service
public class AgentCoreService {

    /**
     * 处理用户消息，返回AI响应
     */
    public String processMessage(ChatSession session, String userMessage, 
                                  Map<String, Object> toolResults) {
        // 1. 添加用户消息到会话
        session.addMessage(ChatMessage.user(userMessage));

        // 2. 构建提示词上下文
        String context = buildContext(session);

        // 3. 决定是否调用工具
        String toolCallDecision = decideToolCall(context);

        // 4. 如果需要调用工具，返回工具调用请求
        if (toolCallDecision != null) {
            return toolCallDecision;
        }

        // 5. 生成最终响应
        String response = generateResponse(session);
        session.addMessage(ChatMessage.assistant(response));

        return response;
    }

    /**
     * 处理工具调用结果
     */
    public String processToolResult(ChatSession session, String toolCallId, 
                                     String toolName, Object result) {
        session.addMessage(ChatMessage.tool(toolCallId, toolName, String.valueOf(result)));
        return generateResponse(session);
    }

    /**
     * 构建上下文
     */
    private String buildContext(ChatSession session) {
        StringBuilder context = new StringBuilder();
        context.append("## 当前旅行偏好\n");
        TravelPreference pref = session.getPreference();
        if (pref != null) {
            context.append(String.format("- 目的地: %s\n", pref.getDestination() != null ? pref.getDestination() : "待确认"));
            context.append(String.format("- 日期: %s 至 %s\n", pref.getStartDate(), pref.getEndDate()));
            context.append(String.format("- 预算: %s\n", pref.getBudgetLevel()));
            context.append(String.format("- 类型: %s\n", pref.getTravelType()));
        }
        context.append("\n## 对话历史\n");
        for (ChatMessage msg : session.getMessages()) {
            context.append(String.format("[%s] %s: %s\n", 
                msg.getCreatedAt(), msg.getRole(), msg.getContent()));
        }
        return context.toString();
    }

    /**
     * 决定是否调用工具（简化版，实际可用LLM判断）
     */
    private String decideToolCall(String context) {
        // 简单规则判断
        if (context.contains("天气") || context.contains("weather")) {
            return "{\"tool\": \"weather\", \"action\": \"query_weather\"}";
        }
        if (context.contains("景点") || context.contains(" attractions")) {
            return "{\"tool\": \"attractions\", \"action\": \"search_attractions\"}";
        }
        if (context.contains("路线") || context.contains("路线规划")) {
            return "{\"tool\": \"route\", \"action\": \"plan_route\"}";
        }
        return null;
    }

    /**
     * 生成响应（实际会调用LLM）
     */
    private String generateResponse(ChatSession session) {
        // 这里会调用Spring AI的ChatClient
        return "好的，我来帮你规划旅行路线。";
    }
}
