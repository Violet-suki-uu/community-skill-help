package com.rita.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 */
/**
 * BackendApplication
 * 作用：Spring Boot 启动入口，启动后端服务并加载全部配置与组件。
 */
@MapperScan("com.rita.community.mapper")   // 扫描 Mapper 接口包
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}

