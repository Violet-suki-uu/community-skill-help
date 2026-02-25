package com.rita.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ReservationConfirmReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
@Data
public class ReservationConfirmReq {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 255)
    private String comment;
}

