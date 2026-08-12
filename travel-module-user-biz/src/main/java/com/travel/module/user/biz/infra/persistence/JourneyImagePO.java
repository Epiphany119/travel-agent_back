package com.travel.module.user.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@Data
@TableName("journey_image")
public class JourneyImagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long journeyId;
    private String imageUrl;
    private String caption;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}