package com.shingu.roadmap.apis.openai.metrics;

import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final OpenAiConfig config;
    private final SecureLogger secureLogger;

    // 기본 메트릭 카운터들
    private Counter requestsTotal;
    private Counter successTotal;
    private Counter errorsTotal;
    private Timer requestDuration;
    private DistributionSummary tokenUsage;

    // 커스텀 메트릭 저장소
    private final ConcurrentHashMap<String, AtomicLong> customCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> customGauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer.Sample> activeSamples = new ConcurrentHashMap<>();

    // 비즈니스 메트릭
    private final AtomicLong totalTokensUsed = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final DoubleAdder avgResponseTime = new DoubleAdder();
    private final AtomicLong responseTimeCount = new AtomicLong(0);

    @PostConstruct
    public void initializeMetrics() {
        // 기본 메트릭 등록
        requestsTotal = Counter.builder("openai.requests.total")
                .description("Total number of OpenAI API requests")
                .register(meterRegistry);

        successTotal = Counter.builder("openai.requests.success")
                .description("Number of successful OpenAI API requests")
                .register(meterRegistry);

        errorsTotal = Counter.builder("openai.requests.errors")
                .description("Number of failed OpenAI API requests")
                .register(meterRegistry);

        requestDuration = Timer.builder("openai.request.duration")
                .description("Duration of OpenAI API requests")
                .register(meterRegistry);

        tokenUsage = DistributionSummary.builder("openai.tokens.usage")
                .description("OpenAI token usage per request")
                .register(meterRegistry);

        // 게이지 메트릭 등록
        Gauge.builder("openai.threads.active", this, obj -> (double) obj.getActiveThreadCount())
                .description("Number of active OpenAI threads")
                .register(meterRegistry);

        // 비즈니스 메트릭 등록
        Gauge.builder("openai.tokens.total", totalTokensUsed, atomicLong -> (double) atomicLong.get())
                .description("Total tokens used")
                .register(meterRegistry);

        Gauge.builder("openai.cache.hit.ratio", this, obj -> obj.getCacheHitRatio())
                .description("Cache hit ratio")
                .register(meterRegistry);

        Gauge.builder("openai.response.time.avg", this, obj -> obj.getAverageResponseTime())
                .description("Average response time")
                .register(meterRegistry);

        secureLogger.logConfigurationEvent("METRICS_INITIALIZED", "OpenAI metrics collector started");
    }

    /**
     * 요청 시작 시 호출
     */
    public Timer.Sample startRequest(String operation) {
        requestsTotal.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        activeSamples.put(operation + ":" + System.currentTimeMillis(), sample);
        return sample;
    }

    /**
     * 요청 성공 시 호출
     */
    public void recordSuccess(String operation, Timer.Sample sample, int tokenUsage, long duration) {
        if (sample != null) {
            sample.stop(Timer.builder("openai.request.duration")
                    .tag("operation", operation)
                    .register(meterRegistry));
        }

        successTotal.increment();

        if (tokenUsage > 0) {
            this.tokenUsage.record(tokenUsage);
            totalTokensUsed.addAndGet(tokenUsage);
        }

        // 평균 응답 시간 계산
        avgResponseTime.add(duration);
        responseTimeCount.incrementAndGet();

        secureLogger.logPerformanceMetric(operation, duration, tokenUsage);
    }

    /**
     * 요청 실패 시 호출
     */
    public void recordError(String operation, Timer.Sample sample, String errorType, String errorMessage) {
        if (sample != null) {
            sample.stop(Timer.builder("openai.request.duration")
                    .tag("operation", operation)
                    .tag("error", "true")
                    .register(meterRegistry));
        }

        errorsTotal.increment();

        // 에러 상세 메트릭
        incrementCustomCounter("error." + errorType, 1);

        secureLogger.logApiError("metrics", operation, errorType, errorMessage);
    }

    /**
     * 캐시 히트 기록
     */
    public void recordCacheHit(String operation, String cacheType) {
        cacheHits.incrementAndGet();
        incrementCustomCounter("cache.hit." + cacheType, 1);

        Counter.builder("openai.cache.hit")
                .tag("operation", operation)
                .tag("cache_type", cacheType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 캐시 미스 기록
     */
    public void recordCacheMiss(String operation, String cacheType) {
        cacheMisses.incrementAndGet();
        incrementCustomCounter("cache.miss." + cacheType, 1);

        Counter.builder("openai.cache.miss")
                .tag("operation", operation)
                .tag("cache_type", cacheType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 비즈니스 메트릭 기록
     */
    public void recordBusinessMetric(String metricName, double value, String... tags) {
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be key-value pairs");
        }

        Tags meterTags = Tags.empty();
        for (int i = 0; i < tags.length; i += 2) {
            meterTags = meterTags.and(tags[i], tags[i + 1]);
        }

        Gauge.builder("openai.business." + metricName, () -> value)
                .tags(meterTags)
                .register(meterRegistry);
    }

    /**
     * 커스텀 카운터 증가
     */
    public void incrementCustomCounter(String name, long value) {
        customCounters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(value);
    }

    /**
     * 커스텀 게이지 설정
     */
    public void setCustomGauge(String name, double value) {
        customGauges.computeIfAbsent(name, k -> new DoubleAdder()).reset();
        customGauges.get(name).add(value);
    }

    /**
     * 주기적 메트릭 수집 및 계산
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void collectPeriodicMetrics() {
        try {
            // 에러율 계산
            double errorRate = calculateErrorRate();
            setCustomGauge("error.rate", errorRate);

            // 처리량 계산 (RPM)
            double throughput = calculateThroughput();
            setCustomGauge("throughput.rpm", throughput);

            // 토큰 사용률 계산
            double tokenUsageRate = calculateTokenUsageRate();
            setCustomGauge("token.usage.rate", tokenUsageRate);

            // 시스템 리소스 사용률
            recordSystemMetrics();

            secureLogger.logPerformanceMetric("periodic_metrics_collection", 0, 0);

        } catch (Exception e) {
            secureLogger.logApiError("metrics", "collectPeriodicMetrics", "COLLECTION_ERROR", e.getMessage());
        }
    }

    /**
     * 에러율 계산
     */
    private double calculateErrorRate() {
        double total = requestsTotal.count();
        double errors = errorsTotal.count();
        return total > 0 ? (errors / total) * 100 : 0.0;
    }

    /**
     * 처리량 계산 (분당 요청 수)
     */
    private double calculateThroughput() {
        // 간단한 계산 - 실제로는 시간 윈도우 기반 계산 필요
        return requestsTotal.count() / Math.max(1, getDurationSinceStart().toMinutes());
    }

    /**
     * 토큰 사용률 계산
     */
    private double calculateTokenUsageRate() {
        double total = requestsTotal.count();
        double tokens = totalTokensUsed.get();
        return total > 0 ? tokens / total : 0.0;
    }

    /**
     * 시스템 메트릭 수집
     */
    private void recordSystemMetrics() {
        // JVM 메트릭
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        setCustomGauge("jvm.memory.used", usedMemory);
        setCustomGauge("jvm.memory.total", totalMemory);
        setCustomGauge("jvm.memory.usage.ratio", (double) usedMemory / totalMemory);

        // 스레드 메트릭
        setCustomGauge("jvm.threads.count", Thread.activeCount());
    }

    /**
     * 메트릭 대시보드용 데이터 조회
     */
    public MetricsSummary getMetricsSummary() {
        return MetricsSummary.builder()
                .totalRequests((long) requestsTotal.count())
                .successfulRequests((long) successTotal.count())
                .failedRequests((long) errorsTotal.count())
                .errorRate(calculateErrorRate())
                .averageResponseTime(getAverageResponseTime())
                .totalTokensUsed(totalTokensUsed.get())
                .cacheHitRatio(getCacheHitRatio())
                .throughputRpm(calculateThroughput())
                .activeThreads(getActiveThreadCount())
                .uptime(getDurationSinceStart())
                .build();
    }

    /**
     * 알림 조건 체크
     */
    @Scheduled(fixedRate = 30000) // 30초마다
    public void checkAlertConditions() {
        double errorRate = calculateErrorRate();
        double avgResponseTime = getAverageResponseTime();

        // 에러율 임계값 체크
        if (errorRate > config.getErrorRateThreshold() * 100) {
            secureLogger.logSecurityEvent("alert", "HIGH_ERROR_RATE",
                    String.format("Error rate %.2f%% exceeds threshold %.2f%%",
                                 errorRate, config.getErrorRateThreshold() * 100));
        }

        // 응답 시간 임계값 체크
        if (avgResponseTime > config.getSlowRequestThreshold().toMillis()) {
            secureLogger.logSecurityEvent("alert", "SLOW_RESPONSE_TIME",
                    String.format("Average response time %.2fms exceeds threshold %dms",
                                 avgResponseTime, config.getSlowRequestThreshold().toMillis()));
        }
    }

    // Helper methods
    private double getCacheHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    private double getAverageResponseTime() {
        long count = responseTimeCount.get();
        return count > 0 ? avgResponseTime.sum() / count : 0.0;
    }

    private int getActiveThreadCount() {
        return activeSamples.size();
    }

    private Duration getDurationSinceStart() {
        // 애플리케이션 시작 시간을 기준으로 계산 (실제 구현 필요)
        return Duration.ofMinutes(1); // 임시 값
    }

    /**
     * 메트릭 요약 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class MetricsSummary {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double errorRate;
        private double averageResponseTime;
        private long totalTokensUsed;
        private double cacheHitRatio;
        private double throughputRpm;
        private int activeThreads;
        private Duration uptime;
    }
}