package com.rita.community.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChatConversationResp
 * 作用：响应数据模型，把后端聚合后的字段返回给前端页面。
 */
@Data
public class ChatConversationResp {
    private Long conversationId;
    private Long skillId;
    private String skillTitle;
    private Long buyerId;
    private Long sellerId;
    private Long peerUserId;
    private String peerName;
    private String peerPhone;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
}

