package com.rita.community.controller;

import com.rita.community.common.Result;
import com.rita.community.dto.ChatBookingReq;
import com.rita.community.dto.ChatConversationResp;
import com.rita.community.dto.ChatMessageResp;
import com.rita.community.dto.ChatOpenReq;
import com.rita.community.dto.ChatSendReq;
import com.rita.community.service.ChatService;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ChatController
 * 作用：会话控制器，处理聊天会话创建、消息拉取和发消息。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/conversations/open")
    public Result<Long> openConversation(@Valid @RequestBody ChatOpenReq req, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(chatService.openConversation(userId, req.getSkillId()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/conversations")
    public Result<List<ChatConversationResp>> listConversations(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(chatService.listConversations(userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<ChatMessageResp>> listMessages(@PathVariable Long conversationId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(chatService.listMessages(userId, conversationId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public Result<Boolean> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatSendReq req,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            chatService.sendText(userId, conversationId, req.getContent());
            return Result.ok(true);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/conversations/{conversationId}/booking")
    public Result<Long> createBooking(
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatBookingReq req,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            Long reservationId = chatService.createBooking(userId, conversationId, req.getAddress(), req.getPhone(), req.getNote());
            return Result.ok(reservationId);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Login required");
        }
        String token = auth.substring(7).trim();
        return JwtUtil.getUserId(token);
    }
}

