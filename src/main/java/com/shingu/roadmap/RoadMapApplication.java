package com.shingu.roadmap;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RoadMapApplication {

    public static void main(String[] args) {
        // 현재 활성화된 Spring 프로파일 가져오기
        String profile = System.getProperty("spring.profiles.active", "dev");

        // 프로파일에 맞는 env 파일 이름 설정
        String envFileName = ".env." + profile;

        // 환경변수 설정
        Dotenv dotenv = Dotenv.configure()
                .filename(envFileName) // 프로파일에 맞는 env 파일 설정
                .ignoreIfMissing()     // 없으면 무시 (에러 방지)
                .load();

        // 모든 키-값을 시스템 속성으로 등록
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        // Spring Boot 애플리케이션 실행
        SpringApplication.run(RoadMapApplication.class, args);
    }

}
