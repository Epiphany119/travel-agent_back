package com.travel.module.note.biz.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 创建/更新笔记文档请求体。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteDocumentRequest {
    /** 标题 */
    private String title = "";
    /** 目的地（可空） */
    private String destination;
    /** 封面图 URL（可空） */
    private String coverUrl;
    /** 可见性: private / link，默认 private */
    private String visibility = "private";
    /** 主题设置 JSON（背景色、文字色、强调色等） */
    private String themeJson;
    /** 完整 Markdown 内容 */
    private String content;
    /** 复制来源的社区帖子 ID；仅用于记录版权来源 */
    private Long sourceSocialNoteId;
}
