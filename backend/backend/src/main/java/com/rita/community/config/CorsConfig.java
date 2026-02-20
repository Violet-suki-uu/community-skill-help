package com.rita.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // 允许的前端地址
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));

        // 允许的方法（包含 OPTIONS 预检）
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许所有请求头
        cfg.setAllowedHeaders(List.of("*"));

        // 如果你以后要带 cookie/authorization，这个建议开
        cfg.setAllowCredentials(true);

        // 暴露响应头（可选）
        cfg.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
