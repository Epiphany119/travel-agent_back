package com.travel.module.agent.biz.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionPO> {
}
