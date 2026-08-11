package com.travel.module.agent.biz.domain.repository;

import com.travel.module.agent.biz.domain.entity.ChatSession;

/**
 * 会话仓储接口
 */
public interface ChatSessionRepository {

    /**
     * 保存会话
     */
    void save(ChatSession session);

    /**
     * 根据ID查询会话
     */
    ChatSession findById(Long id);

    /**
     * 根据会话标识查询
     */
    ChatSession findBySessionId(String sessionId);

    /**
     * 删除会话
     */
    void deleteBySessionId(String sessionId);

    /**
     * 查询用户的所有会话
     */
    java.util.List<ChatSession> findByUserId(Long userId);
}
