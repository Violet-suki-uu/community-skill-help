package com.rita.community.controller;

import com.rita.community.common.Result;
import com.rita.community.dto.LoginReq;
import com.rita.community.dto.RegisterReq;
import com.rita.community.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        value = "/api/auth",
        produces = "application/json"
)
/**
 * AuthController
 * 作用：认证控制器，提供注册与登录接口。
 */
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(
            value = "/register",
            consumes = "application/json",
            produces = "application/json"
    )
    public Result<Void> register(@RequestBody RegisterReq req) {
        try {
            authService.register(req);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping(
            value = "/login",
            consumes = "application/json",
            produces = "application/json"
    )
    public Result<String> login(@RequestBody LoginReq req) {
        try {
            return Result.ok(authService.login(req));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}

