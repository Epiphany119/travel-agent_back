package com.travel.agent.persistence;

import org.apache.ibatis.annotations.*;

/**
 * Agent 问卷会话 Mapper
 */
@Mapper
public interface AgentQuestionnaireMapper {

    @Insert("INSERT INTO agent_questionnaire (session_id, user_id, current_step, answers, data_cache, status) " +
            "VALUES (#{sessionId}, #{userId}, #{currentStep}, #{answers}, #{dataCache}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentQuestionnairePO po);

    @Select("SELECT * FROM agent_questionnaire WHERE session_id = #{sessionId} LIMIT 1")
    AgentQuestionnairePO findBySessionId(@Param("sessionId") String sessionId);

    @Update("UPDATE agent_questionnaire SET " +
            "current_step = #{currentStep}, " +
            "answers = #{answers}, " +
            "data_cache = #{dataCache}, " +
            "status = #{status} " +
            "WHERE session_id = #{sessionId}")
    int updateBySessionId(AgentQuestionnairePO po);
}