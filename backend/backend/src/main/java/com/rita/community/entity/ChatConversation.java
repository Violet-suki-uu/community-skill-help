package com.rita.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChatConversation
 * 作用：数据库实体，与数据表字段一一映射，供 ORM 持久化使用。
 */
@Data
@TableName("chat_conversation")
public class ChatConversation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("skill_id")
    private Long skillId;

    @TableField("buyer_id")
    private Long buyerId;

    @TableField("seller_id")
    private Long sellerId;

    @TableField("last_message")
    private String lastMessage;

    @TableField("last_message_type")
    private String lastMessageType;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

