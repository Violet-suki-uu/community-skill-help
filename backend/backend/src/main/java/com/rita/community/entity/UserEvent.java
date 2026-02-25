package com.rita.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserEvent
 * 作用：数据库实体，与数据表字段一一映射，供 ORM 持久化使用。
 */
@Data
@TableName("user_event")
public class UserEvent {
    private Long id;
    private Long userId;
    private String eventType;
    private Long skillId;
    private String keyword;
    private LocalDateTime createdAt;
}


