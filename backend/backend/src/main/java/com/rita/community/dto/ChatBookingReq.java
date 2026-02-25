package com.rita.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ChatBookingReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
@Data
public class ChatBookingReq {
    @NotBlank
    @Size(max = 255)
    private String address;

    @NotBlank
    @Size(max = 32)
    private String phone;

    @Size(max = 255)
    private String note;
}

