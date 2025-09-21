package com.shingu.roadmap.apis.openai.pool;

import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import reactor.netty.resources.ConnectionProvider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionPoolManager implements HealthIndicator {

    private final OpenAiConfig config;
    private final SecureLogger secureLogger;
    private final MeterRegistry meterRegistry;

    private ConnectionProvider connectionProvider;
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalConnectionsCreated = new AtomicLong(0);
    private final AtomicLong connectionFailures = new AtomicLong(0);

    @PostConstruct
    public void initializeConnectionPool() {
        this.connectionProvider = ConnectionProvider.builder("openai-optimized-pool")
                .maxConnections(config.getMaxConnections())
                .maxIdleTime(config.getMaxIdleTime())
                .maxLifeTime(config.getMaxLifeTime())
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .pendingAcquireMaxCount(config.getMaxConnections() * 2)
                .evictInBackground(Duration.ofSeconds(30)) // 백그라운드 정리
                .lifo() // LIFO 방식으로 연결 재사용 (캐시 효율성 향상)
                .metrics(true) // 메트릭 활성화
                .build();

        registerMetrics();
        secureLogger.logConfigurationEvent("CONNECTION_POOL_INITIALIZED",
                String.format("maxConnections=%d, maxIdleTime=%s, maxLifeTime=%s",
                             config.getMaxConnections(),
                             config.getMaxIdleTime(),
                             config.getMaxLifeTime()));
    }

    private void registerMetrics() {
        // 활성 연결 수
        Gauge.builder("openai.connection.active", activeConnections, atomicLong -> (double) atomicLong.get())
                .description("Number of active OpenAI connections")
                .register(meterRegistry);

        // 총 생성된 연결 수
        Gauge.builder("openai.connection.created.total", totalConnectionsCreated, atomicLong -> (double) atomicLong.get())
                .description("Total number of OpenAI connections created")
                .register(meterRegistry);

        // 연결 실패 수
        Gauge.builder("openai.connection.failures.total", connectionFailures, atomicLong -> (double) atomicLong.get())
                .description("Total number of OpenAI connection failures")
                .register(meterRegistry);

        // 연결 풀 사용률
        Gauge.builder("openai.connection.pool.utilization", this, obj -> obj.getPoolUtilization())
                .description("OpenAI connection pool utilization ratio")
                .register(meterRegistry);
    }

    /**
     * 연결 풀 사용률 계산
     */
    private double getPoolUtilization() {
        return (double) activeConnections.get() / config.getMaxConnections();
    }

    /**
     * 연결 획득 시 메트릭 업데이트
     */
    public void onConnectionAcquired() {
        activeConnections.incrementAndGet();
        totalConnectionsCreated.incrementAndGet();
    }

    /**
     * 연결 해제 시 메트릭 업데이트
     */
    public void onConnectionReleased() {
        activeConnections.decrementAndGet();
    }

    /**
     * 연결 실패 시 메트릭 업데이트
     */
    public void onConnectionFailure() {
        connectionFailures.incrementAndGet();
    }

    /**
     * 주기적 연결 풀 상태 모니터링
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void monitorConnectionPool() {
        long active = activeConnections.get();
        double utilization = getPoolUtilization();

        secureLogger.logPerformanceMetric("connection_pool_monitor", 0,
                                         (int) active);

        // 경고 조건 체크
        if (utilization > 0.8) {
            secureLogger.logApiError("connection_pool", "HIGH_UTILIZATION",
                                   "POOL_WARNING",
                                   String.format("Connection pool utilization: %.2f%%", utilization * 100));
        }

        if (connectionFailures.get() > 0 && System.currentTimeMillis() % 300000 == 0) { // 5분마다
            secureLogger.logApiError("connection_pool", "CONNECTION_FAILURES",
                                   "FAILURE_WARNING",
                                   String.format("Total connection failures: %d", connectionFailures.get()));
        }

        log.debug("Connection pool status - Active: {}, Utilization: {:.2f}%, Total Created: {}, Failures: {}",
                 active, utilization * 100, totalConnectionsCreated.get(), connectionFailures.get());
    }

    /**
     * 연결 풀 최적화
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void optimizeConnectionPool() {
        double utilization = getPoolUtilization();

        // 사용률이 낮으면 유휴 연결 정리 권장
        if (utilization < 0.3) {
            secureLogger.logConfigurationEvent("CONNECTION_POOL_OPTIMIZATION",
                    "Low utilization detected, consider reducing pool size");
        }

        // 사용률이 높으면 풀 크기 증가 권장
        if (utilization > 0.9) {
            secureLogger.logConfigurationEvent("CONNECTION_POOL_OPTIMIZATION",
                    "High utilization detected, consider increasing pool size");
        }
    }

    /**
     * 연결 풀 통계 조회
     */
    public ConnectionPoolStats getPoolStats() {
        return ConnectionPoolStats.builder()
                .maxConnections(config.getMaxConnections())
                .activeConnections((int) activeConnections.get())
                .totalConnectionsCreated(totalConnectionsCreated.get())
                .connectionFailures(connectionFailures.get())
                .utilization(getPoolUtilization())
                .maxIdleTime(config.getMaxIdleTime())
                .maxLifeTime(config.getMaxLifeTime())
                .build();
    }

    /**
     * Health Check 구현
     */
    @Override
    public Health health() {
        try {
            ConnectionPoolStats stats = getPoolStats();
            double utilization = stats.getUtilization();

            Health.Builder healthBuilder = Health.up()
                    .withDetail("activeConnections", stats.getActiveConnections())
                    .withDetail("maxConnections", stats.getMaxConnections())
                    .withDetail("utilization", String.format("%.2f%%", utilization * 100))
                    .withDetail("totalCreated", stats.getTotalConnectionsCreated())
                    .withDetail("failures", stats.getConnectionFailures());

            // 상태 판단
            if (utilization > 0.95) {
                return healthBuilder
                        .status("WARN")
                        .withDetail("warning", "Connection pool near capacity")
                        .build();
            }

            if (stats.getConnectionFailures() > 10) {
                return healthBuilder
                        .status("WARN")
                        .withDetail("warning", "High number of connection failures")
                        .build();
            }

            return healthBuilder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * 연결 풀 강제 정리
     */
    public void forceCleanup() {
        if (connectionProvider != null) {
            // 실제 구현에서는 ConnectionProvider의 정리 메서드 호출
            secureLogger.logConfigurationEvent("CONNECTION_POOL_CLEANUP", "Forced cleanup initiated");
        }
    }

    @PreDestroy
    public void destroy() {
        if (connectionProvider != null) {
            connectionProvider.dispose();
            secureLogger.logConfigurationEvent("CONNECTION_POOL_DESTROYED", "Connection pool disposed");
        }
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    /**
     * 연결 풀 통계 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class ConnectionPoolStats {
        private int maxConnections;
        private int activeConnections;
        private long totalConnectionsCreated;
        private long connectionFailures;
        private double utilization;
        private Duration maxIdleTime;
        private Duration maxLifeTime;
    }
}