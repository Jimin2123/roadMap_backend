package com.shingu.roadmap.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads/images}")
    private String uploadPath;

    /**
     * 업로드된 이미지를 정적 리소스로 제공하기 위한 핸들러 설정
     * URL 패턴: /images/**
     * 파일 위치: file:uploads/images/
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().toString();

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + absolutePath + "/")
                .setCachePeriod(3600); // 1시간 캐시
    }
}
