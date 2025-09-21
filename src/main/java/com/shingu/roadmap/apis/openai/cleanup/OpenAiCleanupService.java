package com.shingu.roadmap.apis.openai.cleanup;

import com.shingu.roadmap.apis.openai.cache.OpenAiCacheService;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import com.shingu.roadmap.apis.openai.pool.ConnectionPoolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiCleanupService {

    private final OpenAiCacheService cacheService;
    private final OpenAiClient openAiClient;
    private final ConnectionPoolManager connectionPoolManager;
    private final SecureLogger secureLogger;

    /**
     * 정기적 메모리 정리 (5분마다)
     */
    @Scheduled(fixedRate = 300000)
    public void performRegularCleanup() {
        try {
            long startTime = System.currentTimeMillis();

            // 1. 만료된 캐시 정리
            cacheService.cleanupExpiredEntries();

            // 2. 메모리 사용량 체크 및 정리
            checkAndCleanMemory();

            // 3. 가비지 컬렉션 유도 (필요시)
            long memoryUsage = getMemoryUsagePercentage();
            if (memoryUsage > 85) {
                System.gc();
                secureLogger.logConfigurationEvent("MEMORY_CLEANUP",
                    String.format("Garbage collection triggered - memory usage: %.1f%%", memoryUsage));
            }

            long duration = System.currentTimeMillis() - startTime;
            secureLogger.logPerformanceMetric("regular_cleanup", duration, 0);

        } catch (Exception e) {
            secureLogger.logApiError("cleanup", "performRegularCleanup",
                                   "CLEANUP_ERROR", e.getMessage());
        }
    }

    /**
     * 메모리 사용량 체크 및 정리
     */
    private void checkAndCleanMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) usedMemory / maxMemory * 100;

        secureLogger.logPerformanceMetric("memory_usage", 0, (int) memoryUsage);

        // 메모리 사용률이 높으면 적극적 정리
        if (memoryUsage > 80) {
            aggressiveCleanup();
        }
    }

    /**
     * 적극적 정리 작업
     */
    private void aggressiveCleanup() {
        secureLogger.logConfigurationEvent("AGGRESSIVE_CLEANUP", "High memory usage detected");

        // 1. 전체 캐시 클리어
        cacheService.clearAllCache();

        // 2. Thread 캐시 일부 정리
        if (openAiClient.getThreadCacheSize() > 100) {
            // 오래된 Thread 캐시 정리 (구현 필요)
            secureLogger.logConfigurationEvent("THREAD_CACHE_CLEANUP",
                "Clearing old thread cache entries");
        }

        // 3. 연결 풀 정리
        connectionPoolManager.forceCleanup();
    }

    /**
     * 메모리 사용률 계산
     */
    private long getMemoryUsagePercentage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        return (usedMemory * 100) / maxMemory;
    }

    /**
     * 주간 정리 작업 (일요일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void performWeeklyCleanup() {
        try {
            secureLogger.logConfigurationEvent("WEEKLY_CLEANUP", "Starting weekly cleanup");

            // 1. 모든 캐시 초기화
            cacheService.clearAllCache();

            // 2. 모든 Thread 무효화
            openAiClient.invalidateAllThreads();

            // 3. 시스템 상태 로깅
            logSystemStatus();

            secureLogger.logConfigurationEvent("WEEKLY_CLEANUP", "Weekly cleanup completed");

        } catch (Exception e) {
            secureLogger.logApiError("cleanup", "performWeeklyCleanup",
                                   "WEEKLY_CLEANUP_ERROR", e.getMessage());
        }
    }

    /**
     * 시스템 상태 로깅
     */
    private void logSystemStatus() {
        // 메모리 상태
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024); // MB
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024); // MB

        // 캐시 상태
        OpenAiCacheService.CacheStats cacheStats = cacheService.getCacheStats();

        // 연결 풀 상태
        ConnectionPoolManager.ConnectionPoolStats poolStats = connectionPoolManager.getPoolStats();

        secureLogger.logPerformanceMetric("system_status", 0, 0);
        log.info("System Status - Memory: {}MB/{}MB, Cache L1: {}/{}, Pool: {}/{}",
                usedMemory, maxMemory,
                cacheStats.getL1Size(), cacheStats.getL1MaxSize(),
                poolStats.getActiveConnections(), poolStats.getMaxConnections());
    }

    /**
     * 응급 정리 (메모리 부족 시 호출)
     */
    public void emergencyCleanup() {
        secureLogger.logSecurityEvent("emergency", "MEMORY_EMERGENCY", "Emergency cleanup triggered");

        try {
            // 1. 모든 캐시 즉시 정리
            cacheService.clearAllCache();

            // 2. 모든 Thread 무효화
            openAiClient.invalidateAllThreads();

            // 3. 강제 가비지 컬렉션
            System.gc();

            // 4. 연결 풀 강제 정리
            connectionPoolManager.forceCleanup();

            secureLogger.logConfigurationEvent("EMERGENCY_CLEANUP", "Emergency cleanup completed");

        } catch (Exception e) {
            secureLogger.logApiError("cleanup", "emergencyCleanup",
                                   "EMERGENCY_CLEANUP_ERROR", e.getMessage());
        }
    }

    /**
     * 애플리케이션 종료 시 정리
     */
    @PreDestroy
    public void shutdown() {
        secureLogger.logConfigurationEvent("SHUTDOWN_CLEANUP", "Starting shutdown cleanup");

        try {
            // 1. 모든 캐시 정리
            cacheService.clearAllCache();

            // 2. 모든 Thread 무효화
            openAiClient.invalidateAllThreads();

            // 3. 연결 풀 정리는 ConnectionPoolManager에서 자동 처리

            secureLogger.logConfigurationEvent("SHUTDOWN_CLEANUP", "Shutdown cleanup completed");

        } catch (Exception e) {
            log.error("Error during shutdown cleanup", e);
        }
    }

    /**
     * 정리 통계 조회
     */
    public CleanupStats getCleanupStats() {
        return CleanupStats.builder()
                .memoryUsagePercentage(getMemoryUsagePercentage())
                .threadCacheSize(openAiClient.getThreadCacheSize())
                .cacheStats(cacheService.getCacheStats())
                .connectionPoolStats(connectionPoolManager.getPoolStats())
                .lastCleanupTime(LocalDateTime.now()) // 실제로는 마지막 정리 시간 추적 필요
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class CleanupStats {
        private long memoryUsagePercentage;
        private int threadCacheSize;
        private OpenAiCacheService.CacheStats cacheStats;
        private ConnectionPoolManager.ConnectionPoolStats connectionPoolStats;
        private LocalDateTime lastCleanupTime;
    }
}