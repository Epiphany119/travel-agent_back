package com.travel.module.user.biz.infra.persistence;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 旅程详情视图对象：包含旅程基本信息 + 途经地点 + 照片
 */
@Data
@Builder
public class JourneyDetailVO {
    private JourneyPO journey;
    private List<JourneyPointPO> points;
    private List<JourneyImagePO> images;
}
