package com.rita.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rita.community.dto.LoginReq;
import com.rita.community.dto.RegisterReq;
import com.rita.community.entity.User;
import com.rita.community.mapper.UserMapper;
import com.rita.community.service.AuthService;
import com.rita.community.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthServiceImpl
 * 作用：认证业务实现，处理账号注册、密码校验和令牌签发。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
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
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone()));
        if (u == null) {
            throw new RuntimeException("账号或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }

        return JwtUtil.generateToken(u.getId(), u.getRole());
    }
}
