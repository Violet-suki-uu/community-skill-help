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
        authService.register(req);
        return Result.ok(null);
    }

    @PostMapping(
            value = "/login",
            consumes = "application/json",
            produces = "application/json"
    )
    public Result<String> login(@RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }
}
