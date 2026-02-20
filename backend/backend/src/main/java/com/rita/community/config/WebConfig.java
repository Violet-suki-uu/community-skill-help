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
                // ✅ 放行登录注册
                .excludePathPatterns("/api/auth/**")
                // ✅ 放行测试接口
                .excludePathPatterns("/test/**", "/ping");
    }
}
