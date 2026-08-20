package com.travel.agent.persistence;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Agent 问卷会话持久化对象。
 * 存储流式问答进度、已规范化的用户回答、以及一路缓存下来的 API 数据。
 */
@Data
public class AgentQuestionnairePO {

    private Long id;
    /** 会话 ID */
    private String sessionId;
    /** 用户唯一标识 */
    private String userId;
    /** 当前提问步骤索引 */
    private Integer currentStep;
    /** 已收集的用户回答（规范化后的 JSON） */
    private String answers;
    /** API 数据缓存（天气/景点等 JSON） */
    private String dataCache;
    /** active-进行中 / completed-已完成 */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}