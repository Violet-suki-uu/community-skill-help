package com.rita.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig
 * 作用：MVC 配置类，注册拦截器并配置哪些路径需要登录。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login")
                .excludePathPatterns("/api/skills", "/api/skills/**")
                .excludePathPatterns("/api/recommend", "/api/recommend/**")
                .excludePathPatterns("/api/upload/**")
                .excludePathPatterns("/test/**", "/ping");
    }
}
