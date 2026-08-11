package com.travel.module.itinerary.biz.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItineraryMapper extends BaseMapper<ItineraryPO> {
}