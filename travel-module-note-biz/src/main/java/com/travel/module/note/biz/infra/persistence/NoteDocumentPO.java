package com.travel.module.note.biz.infra.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 笔记文档实体。一张笔记对应一个文档，内容以 Markdown 文本形式直接存储。
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
    /** 主题设置 JSON（背景色、文字色、强调色等） */
    private String themeJson;
    /** 完整 Markdown 内容 */
    private String content;
    /** 复制来源的社区帖子 ID；原创笔记为空 */
    private Long sourceSocialNoteId;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
