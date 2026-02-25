package com.rita.community.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor())
                .addPathPatterns("/api/**")
                // login/register
                .excludePathPatterns("/api/auth/**")
                // public skills list (allow before login)
                .excludePathPatterns("/api/skills", "/api/skills/**")
                // test endpoints
                .excludePathPatterns("/test/**", "/ping");
    }
}
