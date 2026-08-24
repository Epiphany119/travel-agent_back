package com.travel.module.note.biz.infra.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 笔记文档实体。一张笔记对应一个文档，内容以结构化块（block）形式存于 note_block 表。
 */
@Data
@TableName("note_document")
public class NoteDocumentPO {
    /** 文档 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属用户 */
    private String userId;
    /** 标题 */
    private String title;
    /** 目的地 */
    private String destination;
    /** 封面图 URL */
    private String coverUrl;
    /** 可见性: private / link */
    private String visibility;
    /** 分享 token */
    private String shareToken;
    /** 状态: draft / published */
    private String status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
