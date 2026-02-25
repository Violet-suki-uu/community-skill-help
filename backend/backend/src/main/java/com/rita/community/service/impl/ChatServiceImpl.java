package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rita.community.dto.ChatConversationResp;
import com.rita.community.dto.ChatMessageResp;
import com.rita.community.entity.ChatConversation;
import com.rita.community.entity.ChatMessage;
import com.rita.community.entity.Reservation;
import com.rita.community.entity.Skill;
import com.rita.community.entity.User;
import com.rita.community.mapper.ChatConversationMapper;
import com.rita.community.mapper.ChatMessageMapper;
import com.rita.community.mapper.SkillMapper;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.ChatService;
import com.rita.community.service.ReservationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ChatServiceImpl
 * 作用：聊天业务实现，整合会话、消息、预约并输出前端展示数据。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_BOOKING = "BOOKING";

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final SkillMapper skillMapper;
    private final UserMapper userMapper;
    private final ReservationService reservationService;

    public ChatServiceImpl(
            ChatConversationMapper conversationMapper,
            ChatMessageMapper messageMapper,
            SkillMapper skillMapper,
            UserMapper userMapper,
            ReservationService reservationService
    ) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.skillMapper = skillMapper;
        this.userMapper = userMapper;
        this.reservationService = reservationService;
    }

    @Override
    public Long openConversation(Long currentUserId, Long skillId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("技能不存在");
        }
        Long sellerId = skill.getUserId();
        if (Objects.equals(sellerId, currentUserId)) {
            throw new IllegalArgumentException("不能联系自己发布的技能");
        }

        ChatConversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getSkillId, skillId)
                .eq(ChatConversation::getBuyerId, currentUserId)
                .eq(ChatConversation::getSellerId, sellerId));
        if (existing != null) {
            return existing.getId();
        }

        LocalDateTime now = LocalDateTime.now();
        ChatConversation conversation = new ChatConversation();
        conversation.setSkillId(skillId);
        conversation.setBuyerId(currentUserId);
        conversation.setSellerId(sellerId);
        conversation.setLastMessage("会话已创建");
        conversation.setLastMessageType(TYPE_TEXT);
        conversation.setLastMessageAt(now);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    @Override
    public List<ChatConversationResp> listConversations(Long currentUserId) {
        List<ChatConversation> list = conversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .and(w -> w.eq(ChatConversation::getBuyerId, currentUserId)
                        .or()
                        .eq(ChatConversation::getSellerId, currentUserId))
                .orderByDesc(ChatConversation::getLastMessageAt)
                .orderByDesc(ChatConversation::getUpdatedAt));

        Set<Long> peerIds = new HashSet<>();
        Set<Long> skillIds = new HashSet<>();
        for (ChatConversation c : list) {
            skillIds.add(c.getSkillId());
            peerIds.add(Objects.equals(c.getBuyerId(), currentUserId) ? c.getSellerId() : c.getBuyerId());
        }

        Map<Long, User> userMap = peerIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(peerIds).stream().collect(Collectors.toMap(User::getId, x -> x));
        Map<Long, Skill> skillMap = skillIds.isEmpty()
                ? Collections.emptyMap()
                : skillMapper.selectBatchIds(skillIds).stream().collect(Collectors.toMap(Skill::getId, x -> x));

        List<ChatConversationResp> result = new ArrayList<>();
        for (ChatConversation c : list) {
            Long peerId = Objects.equals(c.getBuyerId(), currentUserId) ? c.getSellerId() : c.getBuyerId();
            User peer = userMap.get(peerId);
            Skill skill = skillMap.get(c.getSkillId());

            ChatConversationResp resp = new ChatConversationResp();
            resp.setConversationId(c.getId());
            resp.setSkillId(c.getSkillId());
            resp.setBuyerId(c.getBuyerId());
            resp.setSellerId(c.getSellerId());
            resp.setSkillTitle(skill == null ? "技能" : skill.getTitle());
            resp.setPeerUserId(peerId);
            resp.setPeerName(resolveUserName(peer));
            resp.setPeerPhone(peer == null ? null : peer.getPhone());
            resp.setLastMessage(c.getLastMessage());
            resp.setLastMessageAt(c.getLastMessageAt());
            result.add(resp);
        }
        return result;
    }

    @Override
    public List<ChatMessageResp> listMessages(Long currentUserId, Long conversationId) {
        requireParticipant(currentUserId, conversationId);
        List<ChatMessage> list = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreatedAt)
                .orderByAsc(ChatMessage::getId));

        List<ChatMessageResp> result = new ArrayList<>();
        for (ChatMessage m : list) {
            ChatMessageResp resp = new ChatMessageResp();
            resp.setId(m.getId());
            resp.setConversationId(m.getConversationId());
            resp.setSenderId(m.getSenderId());
            resp.setContent(m.getContent());
            resp.setMessageType(m.getMessageType());
            resp.setBookingAddress(m.getBookingAddress());
            resp.setBookingPhone(m.getBookingPhone());
            resp.setNote(m.getNote());
            resp.setCreatedAt(m.getCreatedAt());
            result.add(resp);
        }
        return result;
    }

    @Override
    public void sendText(Long currentUserId, Long conversationId, String content) {
        ChatConversation conversation = requireParticipant(currentUserId, conversationId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setContent(text);
        message.setMessageType(TYPE_TEXT);
        message.setCreatedAt(now);
        messageMapper.insert(message);

        conversation.setLastMessage(shorten(text, 120));
        conversation.setLastMessageType(TYPE_TEXT);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);
    }

    @Override
    public Long createBooking(Long currentUserId, Long conversationId, String address, String phone, String note) {
        ChatConversation conversation = requireParticipant(currentUserId, conversationId);
        if (!Objects.equals(conversation.getBuyerId(), currentUserId)) {
            throw new IllegalArgumentException("只有买家可以发起预约");
        }

        String finalAddress = address == null ? "" : address.trim();
        String finalPhone = phone == null ? "" : phone.trim();
        if (finalAddress.isEmpty() || finalPhone.isEmpty()) {
            throw new IllegalArgumentException("预约地址和电话不能为空");
        }

        Reservation reservation = reservationService.createReservation(
                conversation.getSkillId(),
                conversation.getBuyerId(),
                conversation.getSellerId(),
                conversation.getId(),
                finalAddress,
                finalPhone,
                note
        );

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setMessageType(TYPE_BOOKING);
        message.setContent("预约服务");
        message.setBookingAddress(finalAddress);
        message.setBookingPhone(finalPhone);
        message.setNote(note == null ? null : note.trim());
        message.setCreatedAt(now);
        messageMapper.insert(message);

        conversation.setLastMessage("[预约] " + shorten(finalAddress, 40));
        conversation.setLastMessageType(TYPE_BOOKING);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);

        return reservation.getId();
    }

    private ChatConversation requireParticipant(Long currentUserId, Long conversationId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!Objects.equals(conversation.getBuyerId(), currentUserId) && !Objects.equals(conversation.getSellerId(), currentUserId)) {
            throw new IllegalArgumentException("无会话访问权限");
        }
        return conversation;
    }

    private String resolveUserName(User user) {
        if (user == null) return "用户";
        if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname();
        if (user.getPhone() != null && !user.getPhone().isBlank()) return user.getPhone();
        return "用户";
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max);
    }
}

