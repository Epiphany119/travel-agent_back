package com.travel.module.agent.biz.domain.service;

import com.travel.module.agent.biz.domain.entity.ChatMessage;
import com.travel.module.agent.biz.domain.entity.ChatSession;
import com.travel.module.agent.biz.domain.entity.TravelPreference;
import com.travel.module.user.biz.application.service.UserPreferenceApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Agent核心服务 - 实现ReAct模式的推理和执行
 * 
 * PR#4 增强：集成用户偏好上下文
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCoreService {

    private final UserPreferenceApplicationService preferenceService;
    
    // 用于缓存用户偏好，避免每次都查询数据库
    private static final String PREFERENCE_CACHE_KEY = "_user_preference_";

    /**
     * 处理用户消息，返回AI响应
     * 
     * @param session 会话
     * @param userMessage 用户消息
     * @param toolResults 工具调用结果
     * @return AI 响应
     */
    public String processMessage(ChatSession session, String userMessage, 
                                 Map<String, Object> toolResults) {
        // 1. 添加用户消息到会话
        session.addMessage(ChatMessage.user(userMessage));

        // 2. 构建提示词上下文（包含用户偏好）
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
     * 构建上下文 - PR#4 增强
     * 
     * 包含：
     * 1. 用户旅行偏好（从数据库读取）
     * 2. 当前会话偏好设置
     * 3. 对话历史
     */
    private String buildContext(ChatSession session) {
        StringBuilder context = new StringBuilder();
        
        // ==================== 用户偏好（PR#4 新增）====================
        Long userId = session.getUserId();
        if (userId != null) {
            // 从数据库获取用户偏好
            String userPreference = getUserPreferenceSummary(String.valueOf(userId));
            if (userPreference != null && !userPreference.isBlank()) {
                context.append(userPreference);
            }
        }
        
        // ==================== 会话级偏好设置 ====================
        context.append("\n## 本次会话偏好\n");
        TravelPreference sessionPref = session.getPreference();
        if (sessionPref != null) {
            context.append(String.format("- 目的地: %s\n", 
                sessionPref.getDestination() != null ? sessionPref.getDestination() : "待确认"));
            context.append(String.format("- 日期: %s 至 %s\n", 
                sessionPref.getStartDate(), sessionPref.getEndDate()));
            context.append(String.format("- 预算: %s\n", sessionPref.getBudgetLevel()));
            context.append(String.format("- 类型: %s\n", sessionPref.getTravelType()));
            if (sessionPref.getTravelers() != null) {
                context.append(String.format("- 同行人数: %d 人\n", sessionPref.getTravelers()));
            }
            if (sessionPref.getInterests() != null && sessionPref.getInterests().length > 0) {
                context.append(String.format("- 兴趣: %s\n", String.join(", ", sessionPref.getInterests())));
            }
        } else {
            context.append("- 尚未设置会话偏好\n");
        }
        
        // ==================== 对话历史 ====================
        context.append("\n## 对话历史\n");
        for (ChatMessage msg : session.getMessages()) {
            context.append(String.format("[%s] %s: %s\n", 
                msg.getCreatedAt(), msg.getRole(), msg.getContent()));
        }
        
        return context.toString();
    }

    /**
     * 获取用户偏好摘要 - PR#4 新增
     * 
     * 从用户偏好服务获取用户的详细偏好设置，
     * 用于在 Agent 对话时注入上下文
     */
    private String getUserPreferenceSummary(String userId) {
        try {
            return preferenceService.getPreferenceSummary(userId);
        } catch (Exception e) {
            log.warn("获取用户偏好失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 决定是否调用工具（简化版，实际可用LLM判断）
     */
    private String decideToolCall(String context) {
        // 简单规则判断
        if (context.contains("天气") || context.contains("weather")) {
            return "{\"tool\": \"weather\", \"action\": \"query_weather\"}";
        }
        if (context.contains("景点") || context.contains("attractions")) {
            return "{\"tool\": \"attractions\", \"action\": \"search_attractions\"}";
        }
        if (context.contains("路线") || context.contains("路线规划")) {
            return "{\"tool\": \"route\", \"action\": \"plan_route\"}";
        }
        if (context.contains("餐厅") || context.contains("餐饮") || context.contains("美食")) {
            return "{\"tool\": \"meal\", \"action\": \"search_meal\"}";
        }
        if (context.contains("预算") || context.contains("费用")) {
            return "{\"tool\": \"budget\", \"action\": \"estimate_budget\"}";
        }
        return null;
    }

    /**
     * 生成响应（实际会调用LLM）
     * 
     * PR#4 增强：可以根据用户偏好生成更加个性化的响应
     */
    private String generateResponse(ChatSession session) {
        // 这里会调用Spring AI的ChatClient
        // 传入用户偏好上下文，生成个性化响应
        
        TravelPreference pref = session.getPreference();
        Long userId = session.getUserId();
        
        // 获取用户偏好信息用于个性化回复
        StringBuilder personalization = new StringBuilder();
        if (userId != null) {
            String userPrefSummary = getUserPreferenceSummary(String.valueOf(userId));
            if (userPrefSummary != null && !userPrefSummary.isBlank()) {
                personalization.append("根据您的偏好设置，");
            }
        }
        
        // 根据会话状态生成不同回复
        if (pref == null || (pref.getDestination() == null && session.getMessages().size() <= 2)) {
            return personalization + "很高兴为您服务！请问您想去哪里旅行呢？";
        }
        
        if (pref.getDestination() != null && pref.getDays() == 0) {
            return personalization + "好的，您的目的地是" + pref.getDestination() + "。请问计划停留几天呢？";
        }
        
        if (pref.getDestination() != null && pref.getDays() > 0) {
            return personalization + "明白了，" + pref.getDestination() + pref.getDays() + "日游。让我为您查询相关信息...";
        }
        
        return personalization + "好的，我来帮您规划旅行路线。";
    }
}
