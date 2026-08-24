package com.travel.module.note.biz.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.travel.module.note.biz.infra.persistence.NoteBlockPO;

/** 笔记内容块 Mapper */
@Mapper
public interface NoteBlockMapper extends BaseMapper<NoteBlockPO> {
}
