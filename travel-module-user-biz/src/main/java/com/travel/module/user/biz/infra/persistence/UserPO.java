package com.travel.module.user.biz.infra.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户持久化对象
 */
@Data
@TableName("`user`")
public class UserPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String frequentDestination;
    private String defaultPreferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}