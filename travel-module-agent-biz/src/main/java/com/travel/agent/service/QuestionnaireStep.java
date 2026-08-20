package com.travel.agent.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问卷步骤定义。
 * <p>固定的流式问卷步骤：每步向用户抛出一个问题，用户回答后由 LLM
 * 将自由文本规范化成参数，按需触发对应 API（天气 / 高德），并缓存结果。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireStep {

    /** 步骤索引（从 0 开始） */
    private int index;
    /** 字段标识，用于合并进 answers */
    private String field;
    /** 展现给用户的问题文案 */
    private String question;
    /** 输入类型: text(自由文本), select(单选) */
    private String type;
    /** select 类型的可选值 */
    private List<String> options;
    /** 是否需要 LLM 规范化切割（true 表示把自然语言解析成结构化字段） */
    private boolean llmParse;
    /**
     * 本步骤完成后是否要调用哪个 API: 
     * weather-查天气, poi-搜景点, meal-餐食, none-不触发
     */
    private String apiTrigger;
}