package com.travel.module.note.biz.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 笔记文档响应体（含内容块）。
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<NoteBlockResponse> blocks;
}
