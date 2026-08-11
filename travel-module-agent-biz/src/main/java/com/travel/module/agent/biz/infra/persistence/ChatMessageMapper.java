package com.travel.module.agent.biz.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessagePO> {
}
