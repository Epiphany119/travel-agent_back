package com.travel.module.note.biz.infra.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.travel.module.note.biz.infra.persistence.NoteDocumentPO;

/** 笔记文档 Mapper */
@Mapper
public interface NoteDocumentMapper extends BaseMapper<NoteDocumentPO> {
}
