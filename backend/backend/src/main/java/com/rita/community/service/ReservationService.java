package com.rita.community.service;

import com.rita.community.dto.ReservationResp;
import com.rita.community.entity.Reservation;

import java.util.List;

/**
 * ReservationService
 * 作用：预约业务接口，定义预约创建、取消、完成和评分能力。
 */
public interface ReservationService {
    Reservation createReservation(
            Long skillId,
            Long buyerId,
            Long sellerId,
            Long conversationId,
            String address,
            String phone,
            String note
    );

    List<ReservationResp> listBySkillForUser(Long skillId, Long currentUserId);

    List<ReservationResp> listMine(Long currentUserId, String role);

    boolean cancel(Long reservationId, Long currentUserId);

    boolean sellerFinish(Long reservationId, Long currentUserId);

    boolean buyerConfirm(Long reservationId, Long currentUserId, Integer rating, String comment);
}

