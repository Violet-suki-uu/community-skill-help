package com.rita.community.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkillWithSellerResp {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer status;
    private String imageUrl;
    private String sellerNickname;
    private Integer sellerCreditScore;
}
