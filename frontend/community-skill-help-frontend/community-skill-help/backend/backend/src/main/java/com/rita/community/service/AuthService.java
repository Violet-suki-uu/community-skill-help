package com.rita.community.service;

import com.rita.community.dto.LoginReq;
import com.rita.community.dto.RegisterReq;

public interface AuthService {
    void register(RegisterReq req);
    String login(LoginReq req);
}
