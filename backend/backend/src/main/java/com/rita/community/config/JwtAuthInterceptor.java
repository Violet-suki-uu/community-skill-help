package com.rita.community.config;

import com.rita.community.service.CacheService;
import com.rita.community.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JwtAuthInterceptor
 * 作用：JWT 登录拦截器，在进入业务接口前校验令牌，并通过 Redis 黑名单实现登出。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final CacheService cacheService;

    public JwtAuthInterceptor(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if ("/api/skills".equals(path) || path.startsWith("/api/skills/")) {
            return true;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response, "Unauthorized");
            return false;
        }

        String token = auth.substring(7).trim();
        try {
            JwtUtil.parse(token);
        } catch (Exception e) {
            writeUnauthorized(response, "Invalid token");
            return false;
        }

        if (cacheService.isTokenBlacklisted(token)) {
            writeUnauthorized(response, "Token has been revoked");
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
