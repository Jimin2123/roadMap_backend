package com.shingu.roadmap.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리를 위한 설정 클래스
 * Spring의 @Async 어노테이션을 사용한 비동기 메서드 실행을 지원합니다.
 */
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig {

    /**
     * 진단 작업을 위한 비동기 실행자 설정
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = "diagnosisTaskExecutor")
    public Executor diagnosisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("diagnosis-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Initialized diagnosis task executor with corePoolSize=3, maxPoolSize=10");
        return executor;
    }
}
