package com.rita.community.service;

import com.rita.community.dto.LoginReq;
import com.rita.community.dto.RegisterReq;

/**
 * AuthService
 * 作用：认证业务接口，定义注册、登录与登出能力。
 */
public interface AuthService {
    void register(RegisterReq req);
    String login(LoginReq req);

    /**
     * 登出：把当前 Token 写入 Redis 黑名单，直到自身过期。
     */
    void logout(String token);
}
