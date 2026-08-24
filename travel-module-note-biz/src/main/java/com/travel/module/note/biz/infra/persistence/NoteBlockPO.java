package com.travel.module.note.biz.infra.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 笔记内容块。类似飞书文档的 block：按顺序排列，每个块拥有类型与文本/属性。
 */
@Data
@TableName("note_block")
public class NoteBlockPO {
    /** 块 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属文档 ID */
    private Long documentId;
    /** 块类型: h1/h2/p/list/todo/image/callout/code */
    private String type;
    /** 块的文本内容（含内联 HTML 标记） */
    private String text;
    /** 排序序号 */
    private Integer sortOrder;
    /** 附加属性（如图片 URL、todo 勾选）以 JSON 存储，可空 */
    private String attrsJson;
}
