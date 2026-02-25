package com.rita.community.controller;

import com.rita.community.common.Result;
import com.rita.community.dto.ReservationConfirmReq;
import com.rita.community.dto.ReservationResp;
import com.rita.community.service.ReservationService;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ReservationController
 * 作用：预约控制器，处理预约状态流转与查询。
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/skill/{skillId}")
    public Result<List<ReservationResp>> listBySkill(
            @PathVariable("skillId") Long skillId,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(reservationService.listBySkillForUser(skillId, userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/mine")
    public Result<List<ReservationResp>> listMine(
            @RequestParam(defaultValue = "all") String role,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(reservationService.listMine(userId, role));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PatchMapping("/{reservationId}/cancel")
    public Result<Boolean> cancel(
            @PathVariable("reservationId") Long reservationId,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(reservationService.cancel(reservationId, userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PatchMapping("/{reservationId}/finish")
    public Result<Boolean> finish(
            @PathVariable("reservationId") Long reservationId,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(reservationService.sellerFinish(reservationId, userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PatchMapping("/{reservationId}/confirm")
    public Result<Boolean> confirm(
            @PathVariable("reservationId") Long reservationId,
            @Valid @RequestBody ReservationConfirmReq req,
            HttpServletRequest request
    ) {
        try {
            Long userId = getCurrentUserId(request);
            return Result.ok(reservationService.buyerConfirm(reservationId, userId, req.getRating(), req.getComment()));
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

