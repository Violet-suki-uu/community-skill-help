package com.rita.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkillCreateReq {
    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String category;

    @NotNull
    private BigDecimal price;

    private String imageUrl;

}
