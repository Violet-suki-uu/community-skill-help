package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rita.community.dto.ReservationResp;
import com.rita.community.entity.ChatConversation;
import com.rita.community.entity.ChatMessage;
import com.rita.community.entity.Reservation;
import com.rita.community.entity.Skill;
import com.rita.community.entity.User;
import com.rita.community.mapper.ChatConversationMapper;
import com.rita.community.mapper.ChatMessageMapper;
import com.rita.community.mapper.ReservationMapper;
import com.rita.community.mapper.SkillMapper;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * ReservationServiceImpl
 * 作用：预约业务实现，管理预约状态流转并同步信誉分变化。
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    public static final String ROLE_BUYER = "buyer";
    public static final String ROLE_SELLER = "seller";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_AWAIT_BUYER_CONFIRM = "AWAIT_BUYER_CONFIRM";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELED = "CANCELED";

    private static final String MSG_TYPE_TEXT = "TEXT";
    private static final int SCORE_MIN = 0;
    private static final int SCORE_MAX = 100;
    private static final int BUYER_CANCEL_DELTA = -1;

    private final ReservationMapper reservationMapper;
    private final UserMapper userMapper;
    private final SkillMapper skillMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatConversationMapper chatConversationMapper;

    public ReservationServiceImpl(
            ReservationMapper reservationMapper,
            UserMapper userMapper,
            SkillMapper skillMapper,
            ChatMessageMapper chatMessageMapper,
            ChatConversationMapper chatConversationMapper
    ) {
        this.reservationMapper = reservationMapper;
        this.userMapper = userMapper;
        this.skillMapper = skillMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatConversationMapper = chatConversationMapper;
    }

    @Override
    @Transactional
    public Reservation createReservation(
            Long skillId,
            Long buyerId,
            Long sellerId,
            Long conversationId,
            String address,
            String phone,
            String note
    ) {
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation();
        reservation.setSkillId(skillId);
        reservation.setBuyerId(buyerId);
        reservation.setSellerId(sellerId);
        reservation.setStatus(STATUS_PENDING);
        reservation.setAddress(address == null ? null : address.trim());
        reservation.setPhone(phone == null ? null : phone.trim());
        reservation.setNote(note == null ? null : note.trim());
        reservation.setConversationId(conversationId);
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);
        reservationMapper.insert(reservation);
        return reservation;
    }

    @Override
    public List<ReservationResp> listBySkillForUser(Long skillId, Long currentUserId) {
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSkillId, skillId)
                .and(w -> w.eq(Reservation::getBuyerId, currentUserId).or().eq(Reservation::getSellerId, currentUserId))
                .orderByDesc(Reservation::getCreatedAt));
        return mapReservations(reservations);
    }

    @Override
    public List<ReservationResp> listMine(Long currentUserId, String role) {
        String normalizedRole = role == null ? "" : role.trim().toLowerCase();
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (ROLE_BUYER.equals(normalizedRole)) {
            wrapper.eq(Reservation::getBuyerId, currentUserId);
        } else if (ROLE_SELLER.equals(normalizedRole)) {
            wrapper.eq(Reservation::getSellerId, currentUserId);
        } else {
            wrapper.and(w -> w.eq(Reservation::getBuyerId, currentUserId).or().eq(Reservation::getSellerId, currentUserId));
        }
        wrapper.orderByDesc(Reservation::getCreatedAt);
        return mapReservations(reservationMapper.selectList(wrapper));
    }

    @Override
    @Transactional
    public boolean cancel(Long reservationId, Long currentUserId) {
        Reservation reservation = requireReservation(reservationId);
        boolean isBuyer = Objects.equals(reservation.getBuyerId(), currentUserId);
        boolean isSeller = Objects.equals(reservation.getSellerId(), currentUserId);
        if (!isBuyer && !isSeller) {
            throw new IllegalArgumentException("无权限取消该预约");
        }

        String status = normalizeStatus(reservation.getStatus());
        if (STATUS_COMPLETED.equals(status) || STATUS_CANCELED.equals(status)) {
            return true;
        }

        reservation.setStatus(STATUS_CANCELED);
        reservation.setUpdatedAt(LocalDateTime.now());
        boolean ok = reservationMapper.updateById(reservation) > 0;
        if (!ok) {
            return false;
        }

        if (isBuyer) {
            adjustUserCredit(reservation.getBuyerId(), BUYER_CANCEL_DELTA);
        }

        appendSystemMessage(reservation, currentUserId, "我已取消本次预约。");
        return true;
    }

    @Override
    @Transactional
    public boolean sellerFinish(Long reservationId, Long currentUserId) {
        Reservation reservation = requireReservation(reservationId);
        if (!Objects.equals(reservation.getSellerId(), currentUserId)) {
            throw new IllegalArgumentException("仅卖家可操作完成");
        }

        String status = normalizeStatus(reservation.getStatus());
        if (STATUS_COMPLETED.equals(status) || STATUS_CANCELED.equals(status)) {
            return true;
        }
        if (!STATUS_PENDING.equals(status)) {
            throw new IllegalArgumentException("当前状态不可标记完成");
        }

        reservation.setStatus(STATUS_AWAIT_BUYER_CONFIRM);
        reservation.setUpdatedAt(LocalDateTime.now());
        boolean ok = reservationMapper.updateById(reservation) > 0;
        if (!ok) {
            return false;
        }

        appendSystemMessage(reservation, currentUserId, "我已完成本次服务，请确认。");
        return true;
    }

    @Override
    @Transactional
    public boolean buyerConfirm(Long reservationId, Long currentUserId, Integer rating, String comment) {
        Reservation reservation = requireReservation(reservationId);
        if (!Objects.equals(reservation.getBuyerId(), currentUserId)) {
            throw new IllegalArgumentException("仅买家可确认完成");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分需为1-5星");
        }

        String status = normalizeStatus(reservation.getStatus());
        if (STATUS_COMPLETED.equals(status)) {
            return true;
        }
        if (!STATUS_AWAIT_BUYER_CONFIRM.equals(status)) {
            throw new IllegalArgumentException("当前状态不可确认完成");
        }

        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(STATUS_COMPLETED);
        reservation.setRating(rating);
        reservation.setRatingComment(comment == null ? null : trimToEmpty(comment));
        reservation.setRatedAt(now);
        reservation.setUpdatedAt(now);
        boolean ok = reservationMapper.updateById(reservation) > 0;
        if (!ok) {
            return false;
        }

        adjustUserCredit(reservation.getSellerId(), toSellerCreditDelta(rating));
        appendSystemMessage(reservation, currentUserId, "我已确认本次服务完成。");
        return true;
    }

    private Reservation requireReservation(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        return reservation;
    }

    private int toSellerCreditDelta(int rating) {
        return switch (rating) {
            case 5 -> 6;
            case 4 -> 3;
            case 3 -> 1;
            case 2 -> -3;
            case 1 -> -6;
            default -> 0;
        };
    }

    private void adjustUserCredit(Long userId, int delta) {
        if (userId == null || delta == 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int base = user.getCreditScore() == null ? 60 : user.getCreditScore();
        int next = clamp(base + delta, SCORE_MIN, SCORE_MAX);
        user.setCreditScore(next);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<ReservationResp> mapReservations(List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> skillIds = new HashSet<>();
        for (Reservation r : reservations) {
            if (r.getBuyerId() != null) userIds.add(r.getBuyerId());
            if (r.getSellerId() != null) userIds.add(r.getSellerId());
            if (r.getSkillId() != null) skillIds.add(r.getSkillId());
        }

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        Map<Long, Skill> skillMap = new HashMap<>();
        if (!skillIds.isEmpty()) {
            List<Skill> skills = skillMapper.selectBatchIds(skillIds);
            for (Skill skill : skills) {
                skillMap.put(skill.getId(), skill);
            }
        }

        List<ReservationResp> result = new ArrayList<>();
        for (Reservation r : reservations) {
            ReservationResp resp = new ReservationResp();
            resp.setId(r.getId());
            resp.setSkillId(r.getSkillId());
            Skill skill = skillMap.get(r.getSkillId());
            resp.setSkillTitle(skill == null ? "技能" : skill.getTitle());
            resp.setBuyerId(r.getBuyerId());
            resp.setSellerId(r.getSellerId());
            resp.setBuyerName(resolveUserName(userMap.get(r.getBuyerId())));
            resp.setSellerName(resolveUserName(userMap.get(r.getSellerId())));
            resp.setStatus(r.getStatus());
            resp.setAddress(r.getAddress());
            resp.setPhone(r.getPhone());
            resp.setNote(r.getNote());
            resp.setRating(r.getRating());
            resp.setRatingComment(r.getRatingComment());
            resp.setRatedAt(r.getRatedAt());
            resp.setConversationId(r.getConversationId());
            resp.setCreatedAt(r.getCreatedAt());
            resp.setUpdatedAt(r.getUpdatedAt());
            result.add(resp);
        }
        return result;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "";
        return status.trim().toUpperCase();
    }

    private void appendSystemMessage(Reservation reservation, Long senderId, String content) {
        if (reservation.getConversationId() == null) {
            return;
        }
        ChatConversation conversation = chatConversationMapper.selectById(reservation.getConversationId());
        if (conversation == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setMessageType(MSG_TYPE_TEXT);
        message.setContent(content);
        message.setCreatedAt(now);
        chatMessageMapper.insert(message);

        conversation.setLastMessage(content);
        conversation.setLastMessageType(MSG_TYPE_TEXT);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        chatConversationMapper.updateById(conversation);
    }

    private String resolveUserName(User user) {
        if (user == null) return "User";
        if (!isBlank(user.getNickname())) return user.getNickname();
        if (!isBlank(user.getPhone())) return user.getPhone();
        return "User";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

