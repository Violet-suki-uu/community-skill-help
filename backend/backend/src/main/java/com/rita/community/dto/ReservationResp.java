package com.rita.community.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * ReservationResp
 * 作用：响应数据模型，把后端聚合后的字段返回给前端页面。
 */
@Data
public class ReservationResp {
    private Long id;
    private Long skillId;
    private String skillTitle;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private String status;
    private String address;
    private String phone;
    private String note;
    private Integer rating;
    private String ratingComment;
    private LocalDateTime ratedAt;
    private Long conversationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

