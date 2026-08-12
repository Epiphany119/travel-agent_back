package com.travel.module.user.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("journey_point")
public class JourneyPointPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long journeyId;
    private String name;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private LocalDate visitDate;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}