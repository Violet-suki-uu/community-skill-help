package com.rita.community.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChatMessageResp
 * 作用：响应数据模型，把后端聚合后的字段返回给前端页面。
 */
@Data
public class ChatMessageResp {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String messageType;
    private String bookingAddress;
    private String bookingPhone;
    private String note;
    private LocalDateTime createdAt;
}

