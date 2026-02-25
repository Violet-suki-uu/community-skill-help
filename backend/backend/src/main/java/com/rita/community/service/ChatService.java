package com.rita.community.service;

import com.rita.community.dto.ChatConversationResp;
import com.rita.community.dto.ChatMessageResp;

import java.util.List;

/**
 * ChatService
 * 作用：聊天业务接口，定义会话、消息和预约下单入口。
 */
public interface ChatService {
    Long openConversation(Long currentUserId, Long skillId);

    List<ChatConversationResp> listConversations(Long currentUserId);

    List<ChatMessageResp> listMessages(Long currentUserId, Long conversationId);

    void sendText(Long currentUserId, Long conversationId, String content);

    Long createBooking(Long currentUserId, Long conversationId, String address, String phone, String note);
}

