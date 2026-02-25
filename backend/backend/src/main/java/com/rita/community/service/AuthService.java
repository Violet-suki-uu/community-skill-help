package com.rita.community.service;

import com.rita.community.dto.LoginReq;
import com.rita.community.dto.RegisterReq;

/**
 * AuthService
 * 作用：认证业务接口，定义注册和登录能力。
 */
public interface AuthService {
    void register(RegisterReq req);
    String login(LoginReq req);
}

