package com.shingu.roadmap.apis.openai.health;

import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import com.shingu.roadmap.apis.openai.metrics.OpenAiMetricsCollector;
import com.shingu.roadmap.apis.openai.pool.ConnectionPoolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiHealthIndicator implements HealthIndicator {

    private final OpenAiClient openAiClient;
    private final OpenAiConfig config;
    private final SecureLogger secureLogger;
    private final OpenAiMetricsCollector metricsCollector;
    private final ConnectionPoolManager connectionPoolManager;

    private static final String HEALTH_CHECK_SESSION = "health-check";
    private static final String HEALTH_CHECK_MESSAGE = "ping";
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);

    @Override
    public Health health() {
        try {
            return performComprehensiveHealthCheck();
        } catch (Exception e) {
            secureLogger.logApiError(HEALTH_CHECK_SESSION, "health_check", "HEALTH_CHECK_ERROR", e.getMessage());
            return Health.down()
                    .withDetail("error", "Health check failed")
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
        }
    }

    /**
     * 포괄적인 헬스 체크 수행
     */
    private Health performComprehensiveHealthCheck() {
        Health.Builder healthBuilder = Health.up();

        // 1. 기본 설정 검증
        HealthCheckResult configCheck = checkConfiguration();
        healthBuilder.withDetail("configuration", configCheck.toMap());

        // 2. API 연결성 검증
        HealthCheckResult apiCheck = checkApiConnectivity();
        healthBuilder.withDetail("api_connectivity", apiCheck.toMap());

        // 3. 연결 풀 상태 검증
        HealthCheckResult poolCheck = checkConnectionPool();
        healthBuilder.withDetail("connection_pool", poolCheck.toMap());

        // 4. 메트릭 및 성능 검증
        HealthCheckResult metricsCheck = checkMetricsAndPerformance();
        healthBuilder.withDetail("metrics", metricsCheck.toMap());

        // 5. 캐시 상태 검증
        HealthCheckResult cacheCheck = checkCacheHealth();
        healthBuilder.withDetail("cache", cacheCheck.toMap());

        // 전체 상태 결정
        Status overallStatus = determineOverallStatus(configCheck, apiCheck, poolCheck, metricsCheck, cacheCheck);
        healthBuilder.status(overallStatus);

        // 추가 정보
        healthBuilder
                .withDetail("version", "1.0.0")
                .withDetail("model", config.getModel())
                .withDetail("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .withDetail("check_duration_ms", 0); // 실제 구현에서는 측정

        return healthBuilder.build();
    }

    /**
     * 설정 검증
     */
    private HealthCheckResult checkConfiguration() {
        try {
            boolean apiKeyValid = StringUtils.hasText(config.getApiKey()) && config.getApiKey().length() > 20;
            boolean baseUrlValid = StringUtils.hasText(config.getBaseUrl()) && config.getBaseUrl().startsWith("https://");
            boolean assistantIdValid = StringUtils.hasText(config.getNcsCodeAssistantId());
            boolean temperatureValid = config.getTemperature() >= 0.0 && config.getTemperature() <= 1.0;

            if (apiKeyValid && baseUrlValid && assistantIdValid && temperatureValid) {
                return HealthCheckResult.healthy("All configurations valid")
                        .withDetail("api_key_format", "valid")
                        .withDetail("base_url", config.getBaseUrl())
                        .withDetail("model", config.getModel())
                        .withDetail("temperature", config.getTemperature());
            } else {
                return HealthCheckResult.unhealthy("Invalid configuration detected")
                        .withDetail("api_key_valid", apiKeyValid)
                        .withDetail("base_url_valid", baseUrlValid)
                        .withDetail("assistant_id_valid", assistantIdValid)
                        .withDetail("temperature_valid", temperatureValid);
            }
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Configuration check failed: " + e.getMessage());
        }
    }

    /**
     * API 연결성 검증
     */
    private HealthCheckResult checkApiConnectivity() {
        try {
            String response = openAiClient.askAssistantBlocking(HEALTH_CHECK_SESSION, HEALTH_CHECK_MESSAGE, HEALTH_CHECK_TIMEOUT);

            if (StringUtils.hasText(response)) {
                secureLogger.logApiResponse(HEALTH_CHECK_SESSION, response.length(), HEALTH_CHECK_TIMEOUT.toMillis());

                return HealthCheckResult.healthy("API accessible and responsive")
                        .withDetail("response_received", true)
                        .withDetail("response_length", response.length())
                        .withDetail("response_time", "< " + HEALTH_CHECK_TIMEOUT.toSeconds() + "s");
            } else {
                return HealthCheckResult.unhealthy("API returned empty response");
            }

        } catch (Exception e) {
            secureLogger.logApiError(HEALTH_CHECK_SESSION, "api_connectivity_check", "API_CHECK_FAILED", e.getMessage());

            String errorType = e.getClass().getSimpleName();
            if (errorType.contains("Timeout")) {
                return HealthCheckResult.degraded("API response timeout")
                        .withDetail("timeout", HEALTH_CHECK_TIMEOUT.toString())
                        .withDetail("error_type", "timeout");
            } else {
                return HealthCheckResult.unhealthy("API connectivity failed: " + e.getMessage())
                        .withDetail("error_type", errorType);
            }
        }
    }

    /**
     * 연결 풀 상태 검증
     */
    private HealthCheckResult checkConnectionPool() {
        try {
            ConnectionPoolManager.ConnectionPoolStats stats = connectionPoolManager.getPoolStats();

            boolean poolHealthy = stats.getUtilization() < 0.9 && stats.getConnectionFailures() < 10;

            if (poolHealthy) {
                return HealthCheckResult.healthy("Connection pool operating normally")
                        .withDetail("active_connections", stats.getActiveConnections())
                        .withDetail("max_connections", stats.getMaxConnections())
                        .withDetail("utilization", String.format("%.2f%%", stats.getUtilization() * 100))
                        .withDetail("failures", stats.getConnectionFailures());
            } else {
                String status = stats.getUtilization() >= 0.9 ? "degraded" : "unhealthy";
                return HealthCheckResult.of(status, "Connection pool under stress")
                        .withDetail("utilization", String.format("%.2f%%", stats.getUtilization() * 100))
                        .withDetail("failures", stats.getConnectionFailures())
                        .withDetail("warning", stats.getUtilization() >= 0.9 ? "High utilization" : "High failure count");
            }

        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Connection pool check failed: " + e.getMessage());
        }
    }

    /**
     * 메트릭 및 성능 검증
     */
    private HealthCheckResult checkMetricsAndPerformance() {
        try {
            OpenAiMetricsCollector.MetricsSummary metrics = metricsCollector.getMetricsSummary();

            boolean performanceGood = metrics.getErrorRate() < 5.0 &&
                                    metrics.getAverageResponseTime() < config.getSlowRequestThreshold().toMillis();

            if (performanceGood) {
                return HealthCheckResult.healthy("Performance metrics within acceptable range")
                        .withDetail("error_rate", String.format("%.2f%%", metrics.getErrorRate()))
                        .withDetail("avg_response_time", String.format("%.2fms", metrics.getAverageResponseTime()))
                        .withDetail("total_requests", metrics.getTotalRequests())
                        .withDetail("cache_hit_ratio", String.format("%.2f%%", metrics.getCacheHitRatio() * 100));
            } else {
                return HealthCheckResult.degraded("Performance metrics showing degradation")
                        .withDetail("error_rate", String.format("%.2f%%", metrics.getErrorRate()))
                        .withDetail("avg_response_time", String.format("%.2fms", metrics.getAverageResponseTime()))
                        .withDetail("threshold_error_rate", "5.0%")
                        .withDetail("threshold_response_time", config.getSlowRequestThreshold().toMillis() + "ms");
            }

        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Metrics check failed: " + e.getMessage());
        }
    }

    /**
     * 캐시 상태 검증
     */
    private HealthCheckResult checkCacheHealth() {
        try {
            // 간단한 캐시 테스트
            int cacheSize = openAiClient.getThreadCacheSize();

            return HealthCheckResult.healthy("Cache operating normally")
                    .withDetail("thread_cache_size", cacheSize)
                    .withDetail("max_cache_size", config.getMaxCacheEntries());

        } catch (Exception e) {
            return HealthCheckResult.degraded("Cache check failed: " + e.getMessage());
        }
    }

    /**
     * 전체 상태 결정
     */
    private Status determineOverallStatus(HealthCheckResult... results) {
        for (HealthCheckResult result : results) {
            if ("unhealthy".equals(result.getStatus())) {
                return Status.DOWN;
            }
        }

        for (HealthCheckResult result : results) {
            if ("degraded".equals(result.getStatus())) {
                return new Status("WARN");
            }
        }

        return Status.UP;
    }

    /**
     * 헬스 체크 결과 클래스
     */
    public static class HealthCheckResult {
        private final String status;
        private final String message;
        private final java.util.Map<String, Object> details = new java.util.HashMap<>();

        private HealthCheckResult(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public static HealthCheckResult healthy(String message) {
            return new HealthCheckResult("healthy", message);
        }

        public static HealthCheckResult degraded(String message) {
            return new HealthCheckResult("degraded", message);
        }

        public static HealthCheckResult unhealthy(String message) {
            return new HealthCheckResult("unhealthy", message);
        }

        public static HealthCheckResult of(String status, String message) {
            return new HealthCheckResult(status, message);
        }

        public HealthCheckResult withDetail(String key, Object value) {
            details.put(key, value);
            return this;
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }

        public java.util.Map<String, Object> toMap() {
            java.util.Map<String, Object> map = new java.util.HashMap<>(details);
            map.put("status", status);
            map.put("message", message);
            return map;
        }
    }
}