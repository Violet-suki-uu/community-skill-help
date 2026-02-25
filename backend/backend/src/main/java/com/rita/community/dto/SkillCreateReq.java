package com.rita.community.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SkillCreateReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
@Data
public class SkillCreateReq {
    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String category;

    @NotNull
    private BigDecimal price;

    @JsonAlias({"image", "imageUrl"})
    private String imageUrl;

    private BigDecimal lng;
    private BigDecimal lat;
    private String address;
    private String adcode;
    private String cityName;
}

