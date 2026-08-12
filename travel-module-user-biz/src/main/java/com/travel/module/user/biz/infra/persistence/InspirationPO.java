package com.travel.module.user.biz.infra.persistence;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@Data
@TableName("inspiration")
public class InspirationPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String name;
    private String imageUrl;
    private String quote;
    private String description;
    private String tags;
    private Integer priority;
    private Integer estimatedBudget;
    private String bestSeason;
    private Integer status;
    private LocalDateTime achievedAt;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}