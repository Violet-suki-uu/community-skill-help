package com.rita.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ChatSendReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
@Data
public class ChatSendReq {
    @NotBlank
    @Size(max = 1000)
    private String content;
}

