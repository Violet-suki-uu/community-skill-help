package com.rita.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChatMessage
 * 作用：数据库实体，与数据表字段一一映射，供 ORM 持久化使用。
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    private String content;

    @TableField("message_type")
    private String messageType;

    @TableField("booking_address")
    private String bookingAddress;

    @TableField("booking_phone")
    private String bookingPhone;

    private String note;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

