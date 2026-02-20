package com.rita.community.controller;

import com.rita.community.common.Result;
import com.rita.community.entity.User;
import com.rita.community.mapper.UserMapper;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public Result<UserMeResp> me(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.fail("未登录");
        }

        String token = auth.substring(7);
        Long userId;
        try {
            userId = JwtUtil.getUserId(token);
        } catch (Exception e) {
            return Result.fail("登录已失效，请重新登录");
        }

        User u = userMapper.selectById(userId);
        if (u == null) {
            return Result.fail("用户不存在");
        }

        UserMeResp resp = new UserMeResp();
        resp.setId(u.getId());
        resp.setPhone(u.getPhone());
        resp.setNickname(u.getNickname());
        resp.setRole(u.getRole());
        resp.setCreditScore(u.getCreditScore());

        return Result.ok(resp);
    }

    // 简单响应类（不把 password 返回给前端）
    public static class UserMeResp {
        private Long id;
        private String phone;
        private String nickname;
        private String role;
        private Integer creditScore;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public Integer getCreditScore() { return creditScore; }
        public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
    }
}
