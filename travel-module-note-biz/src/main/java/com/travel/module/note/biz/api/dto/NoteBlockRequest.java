package com.travel.module.note.biz.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 笔记内容块请求体。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteBlockRequest {
    /** 块类型: h1/h2/p/list/todo/image/callout/code */
    @NotNull
    private String type;
    /** 块的文本内容（含内联 HTML 标记，可为空字符串） */
    private String text;
    /** 排序序号，前端在保存时整段重排，服务端按传入顺序重排 */
    private Integer sortOrder;
    /** 附加属性 JSON（如图片 URL、todo 勾选），可空 */
    private String attrsJson;
}
