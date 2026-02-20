package com.rita.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离：先关闭 CSRF（后面做 JWT 再收紧）
                .csrf(csrf -> csrf.disable())

                // 开启 CORS（会读取我们定义的 CorsConfigurationSource）
                .cors(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        //  预检请求 OPTIONS 必须放行，否则前端会 CORS 报错
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 目前先全部放行（等 JWT 完成后再加权限）
                        .anyRequest().permitAll()
                )

                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
