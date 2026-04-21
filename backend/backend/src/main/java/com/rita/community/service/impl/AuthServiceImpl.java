package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rita.community.dto.LoginReq;
import com.rita.community.dto.RegisterReq;
import com.rita.community.entity.User;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.AuthService;
import com.rita.community.service.CacheService;
import com.rita.community.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthServiceImpl
 * 作用：认证业务实现，处理账号注册、密码校验、Token 签发与登出，并接入 Redis 做登录失败限流。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheService cacheService;

    @Value("${app.rate-limit.login-max-fail:5}")
    private int loginMaxFail;

    @Value("${app.rate-limit.login-lock-seconds:300}")
    private int loginLockSeconds;

    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           CacheService cacheService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.cacheService = cacheService;
    }

    @Override
    public void register(RegisterReq req) {
        Long cnt = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone()));
        if (cnt != null && cnt > 0) {
            throw new RuntimeException("手机号已注册");
        }

        User u = new User();
        u.setPhone(req.getPhone());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname());
        u.setRole("USER");
        u.setCreditScore(60);

        userMapper.insert(u);
    }

    @Override
    public String login(LoginReq req) {
        String phone = req.getPhone();
        if (cacheService.isLoginLocked(phone)) {
            throw new RuntimeException("登录失败次数过多，请" + loginLockSeconds / 60 + "分钟后再试");
        }

        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            long count = cacheService.incrLoginFail(phone, loginMaxFail, loginLockSeconds);
            long remain = Math.max(0, loginMaxFail - count);
            if (count >= loginMaxFail) {
                throw new RuntimeException("账号或密码错误，已临时锁定" + loginLockSeconds / 60 + "分钟");
            }
            throw new RuntimeException("账号或密码错误，还剩 " + remain + " 次机会");
        }

        cacheService.resetLoginFail(phone);
        return JwtUtil.generateToken(u.getId(), u.getRole());
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        long remain = JwtUtil.getRemainMs(token);
        cacheService.blacklistToken(token, remain);
    }
}
