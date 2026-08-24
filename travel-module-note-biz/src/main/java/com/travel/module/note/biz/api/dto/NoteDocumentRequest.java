package com.travel.module.note.biz.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

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
    /** 内容块列表，按顺序保存 */
    private List<NoteBlockRequest> blocks = new ArrayList<>();
}
