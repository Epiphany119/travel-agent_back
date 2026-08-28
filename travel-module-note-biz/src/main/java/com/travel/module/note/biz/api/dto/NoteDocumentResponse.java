package com.travel.module.note.biz.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 笔记文档响应体。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoteDocumentResponse {
    private Long id;
    private String userId;
    private String title;
    private String destination;
    private String coverUrl;
    private String visibility;
    private String shareToken;
    private String status;
    /** 主题设置 JSON */
    private String themeJson;
    /** 完整 Markdown 内容 */
    private String content;
    /** 复制来源的社区帖子 ID */
    private Long sourceSocialNoteId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
