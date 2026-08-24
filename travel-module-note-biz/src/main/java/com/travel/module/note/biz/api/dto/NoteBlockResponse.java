package com.travel.module.note.biz.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 笔记内容块响应体。
 */
@Data
@Builder
public class NoteBlockResponse {
    private Long id;
    private String type;
    private String text;
    private Integer sortOrder;
    private String attrsJson;
}
