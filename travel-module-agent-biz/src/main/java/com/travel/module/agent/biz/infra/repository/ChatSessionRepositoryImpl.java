package com.travel.module.agent.biz.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.module.agent.biz.domain.entity.ChatMessage;
import com.travel.module.agent.biz.domain.entity.ChatSession;
import com.travel.module.agent.biz.domain.entity.TravelPreference;
import com.travel.module.agent.biz.domain.repository.ChatSessionRepository;
import com.travel.module.agent.biz.infra.persistence.ChatMessageMapper;
import com.travel.module.agent.biz.infra.persistence.ChatMessagePO;
import com.travel.module.agent.biz.infra.persistence.ChatSessionMapper;
import com.travel.module.agent.biz.infra.persistence.ChatSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话仓储实现
 */
@Repository
@Primary
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(ChatSession session) {
        // 保存会话（insert或update）
        ChatSessionPO sessionPO = toSessionPO(session);
        if (sessionPO.getId() == null) {
            sessionMapper.insert(sessionPO);
            session.setId(sessionPO.getId());
        } else {
            sessionMapper.updateById(sessionPO);
        }

        // 保存消息
        if (session.getMessages() != null) {
            for (ChatMessage msg : session.getMessages()) {
                if (msg.getMessageId() == null) {
                    ChatMessagePO msgPO = toMessagePO(msg, session.getSessionId());
                    messageMapper.insert(msgPO);
                }
            }
        }
    }

    @Override
    public ChatSession findById(Long id) {
        ChatSessionPO sessionPO = sessionMapper.selectById(id);
        if (sessionPO == null) {
            return null;
        }
        return toSession(sessionPO);
    }

    @Override
    public ChatSession findBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPO::getSessionId, sessionId);
        ChatSessionPO sessionPO = sessionMapper.selectOne(wrapper);
        if (sessionPO == null) {
            return null;
        }
        return toSession(sessionPO);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        LambdaQueryWrapper<ChatSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPO::getSessionId, sessionId);
        sessionMapper.delete(wrapper);

        LambdaQueryWrapper<ChatMessagePO> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessagePO::getSessionId, sessionId);
        messageMapper.delete(msgWrapper);
    }

    @Override
    public List<ChatSession> findByUserId(Long userId) {
        LambdaQueryWrapper<ChatSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPO::getUserId, userId);
        wrapper.orderByDesc(ChatSessionPO::getLastActiveAt);
        List<ChatSessionPO> pos = sessionMapper.selectList(wrapper);
        return pos.stream().map(this::toSession).collect(Collectors.toList());
    }

    private ChatSession toSession(ChatSessionPO po) {
        ChatSession session = new ChatSession();
        session.setId(po.getId());
        session.setSessionId(po.getSessionId());
        session.setUserId(po.getUserId());
        session.setTitle(po.getTitle());
        session.setStatus(po.getStatus());
        session.setCreatedAt(po.getCreatedAt());
        session.setLastActiveAt(po.getLastActiveAt());

        // 解析偏好
        if (po.getPreferenceJson() != null) {
            try {
                session.setPreference(objectMapper.readValue(po.getPreferenceJson(), TravelPreference.class));
            } catch (JsonProcessingException e) {
                session.setPreference(null);
            }
        }

        // 查询消息
        LambdaQueryWrapper<ChatMessagePO> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessagePO::getSessionId, po.getSessionId());
        msgWrapper.orderByAsc(ChatMessagePO::getCreatedAt);
        List<ChatMessagePO> msgPOs = messageMapper.selectList(msgWrapper);
        session.setMessages(msgPOs.stream().map(this::toMessage).collect(Collectors.toList()));

        return session;
    }

    private ChatMessage toMessage(ChatMessagePO po) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(po.getMessageId());
        msg.setRole(po.getRole());
        msg.setContent(po.getContent());
        msg.setToolCallId(po.getToolCallId());
        msg.setToolName(po.getToolName());
        msg.setCreatedAt(po.getCreatedAt());
        return msg;
    }

    private ChatSessionPO toSessionPO(ChatSession session) {
        ChatSessionPO po = new ChatSessionPO();
        po.setId(session.getId());
        po.setSessionId(session.getSessionId());
        po.setUserId(session.getUserId());
        po.setTitle(session.getTitle());
        po.setStatus(session.getStatus());
        po.setCreatedAt(session.getCreatedAt());
        po.setLastActiveAt(session.getLastActiveAt());

        if (session.getPreference() != null) {
            try {
                po.setPreferenceJson(objectMapper.writeValueAsString(session.getPreference()));
            } catch (JsonProcessingException e) {
                po.setPreferenceJson(null);
            }
        }
        return po;
    }

    private ChatMessagePO toMessagePO(ChatMessage msg, String sessionId) {
        ChatMessagePO po = new ChatMessagePO();
        if (msg.getMessageId() == null) {
            po.setMessageId(java.util.UUID.randomUUID().toString().replace("-", ""));
        } else {
            po.setMessageId(msg.getMessageId());
        }
        po.setSessionId(sessionId);
        po.setRole(msg.getRole());
        po.setContent(msg.getContent());
        po.setToolCallId(msg.getToolCallId());
        po.setToolName(msg.getToolName());
        po.setCreatedAt(msg.getCreatedAt());
        return po;
    }
}
