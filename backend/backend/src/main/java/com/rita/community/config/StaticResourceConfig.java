package com.rita.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * StaticResourceConfig
 * 作用：静态资源映射配置，把本地 uploads 目录（默认 ./uploads，可通过 app.uploads-dir 指定）映射为可访问 URL。
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.uploads-dir:}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = resolveUploadDir();
        String uploadPath = uploadDir.toString().replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }

    Path resolveUploadDir() {
        if (uploadsDir != null && !uploadsDir.isBlank()) {
            return Paths.get(uploadsDir).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
    }
}
