package com.rita.community.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SkillListItemResp
 * 作用：响应数据模型，把后端聚合后的字段返回给前端页面。
 */
@Data
public class SkillListItemResp {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imageUrl;
    private BigDecimal lng;
    private BigDecimal lat;
    private String address;
    private String adcode;
    private String cityName;
    private Integer viewCount;

    private String sellerNickname;
    private Integer sellerCreditScore;
}

