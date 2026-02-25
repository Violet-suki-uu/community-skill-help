package com.rita.community.common;

import lombok.Data;

/**
 * Result
 * 作用：统一响应包装对象，让前后端用一致的数据结构传递成功或失败结果。
 */
@Data
public class Result<T> {
    private int code; // 0 成功，其他失败
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "OK";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.message = message;
        r.data = null;
        return r;
    }
}

