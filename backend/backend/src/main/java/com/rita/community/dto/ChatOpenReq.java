package com.rita.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ChatOpenReq
 * 作用：请求参数模型，承接前端提交字段并配合注解做基础校验。
 */
@Data
public class ChatOpenReq {
    @NotNull
    private Long skillId;
}

