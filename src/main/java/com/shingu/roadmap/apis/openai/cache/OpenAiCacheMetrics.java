package com.shingu.roadmap.apis.openai.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenAI 캐시 메트릭 수집 및 모니터링
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCacheMetrics {

    private final CacheManager cacheManager;

    // 메트릭 카운터
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    private final AtomicLong totalEvictions = new AtomicLong(0);

    /**
     * 캐시 히트 카운트 증가
     */
    public void incrementHit() {
        totalHits.incrementAndGet();
    }

    /**
     * 캐시 미스 카운트 증가
     */
    public void incrementMiss() {
        totalMisses.incrementAndGet();
    }

    /**
     * 캐시 제거 카운트 증가
     */
    public void incrementEviction() {
        totalEvictions.incrementAndGet();
    }

    /**
     * 캐시 통계 정보 반환
     */
    public CacheStatistics getCacheStatistics() {
        long hits = totalHits.get();
        long misses = totalMisses.get();
        long total = hits + misses;
        double hitRatio = total > 0 ? (double) hits / total : 0.0;

        return CacheStatistics.builder()
                .totalHits(hits)
                .totalMisses(misses)
                .totalEvictions(totalEvictions.get())
                .hitRatio(hitRatio)
                .totalRequests(total)
                .build();
    }

    /**
     * 주기적으로 캐시 통계 로깅 (매 30분)
     */
    @Scheduled(fixedRate = 1800000) // 30분
    public void logCacheStatistics() {
        try {
            CacheStatistics stats = getCacheStatistics();

            log.info("=== OpenAI Cache Statistics ===");
            log.info("Total Requests: {}", stats.getTotalRequests());
            log.info("Cache Hits: {}", stats.getTotalHits());
            log.info("Cache Misses: {}", stats.getTotalMisses());
            log.info("Hit Ratio: {:.2f}%", stats.getHitRatio() * 100);
            log.info("Total Evictions: {}", stats.getTotalEvictions());
            log.info("Cache Efficiency: {}", evaluateCacheEfficiency(stats));

            // 개별 캐시 통계
            logIndividualCacheStats();

            // 성능 알림 및 최적화 제안
            suggestOptimizations(stats);

        } catch (Exception e) {
            log.error("Failed to log cache statistics", e);
        }
    }

    /**
     * 개별 캐시별 통계 로깅
     */
    private void logIndividualCacheStats() {
        String[] openAiCaches = {
            "openai:training-recommendation",
            "openai:ncs-code-recommendation",
            "openai:search-codes",
            "openai:keyword-generation",
            "openai:assistant-thread"
        };

        for (String cacheName : openAiCaches) {
            try {
                var cache = cacheManager.getCache(cacheName);
                if (cache instanceof RedisCache redisCache) {
                    // Redis 캐시의 경우 기본적인 정보만 로깅
                    log.info("Cache [{}]: Available", cacheName);
                } else if (cache != null) {
                    log.info("Cache [{}]: Available (Type: {})", cacheName, cache.getClass().getSimpleName());
                } else {
                    log.warn("Cache [{}]: Not found", cacheName);
                }
            } catch (Exception e) {
                log.error("Failed to get stats for cache: {}", cacheName, e);
            }
        }
    }

    /**
     * 캐시 성능 모니터링 및 경고 체크
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void checkCachePerformance() {
        try {
            CacheStatistics stats = getCacheStatistics();

            // 성능 범위별 경고
            checkHitRatio(stats);
            checkEvictionRate(stats);
            checkCacheHealth(stats);

            // 자동 최적화 제안
            if (shouldSuggestOptimization(stats)) {
                suggestOptimizations(stats);
            }

        } catch (Exception e) {
            log.error("캐시 성능 체크 실패", e);
        }
    }

    /**
     * 히트율 경고 체크
     */
    private void checkHitRatio(CacheStatistics stats) {
        if (stats.getTotalRequests() > 100) {
            if (stats.getHitRatio() < 0.3) {
                log.error("캐시 히트율 매우 낮음: {:.2f}% - 시스템 성능에 심각한 영향",
                         stats.getHitRatio() * 100);
            } else if (stats.getHitRatio() < 0.5) {
                log.warn("캐시 히트율 낮음: {:.2f}% - 최적화 검토 필요",
                        stats.getHitRatio() * 100);
            }
        }
    }

    /**
     * 제거율 경고 체크
     */
    private void checkEvictionRate(CacheStatistics stats) {
        if (stats.getTotalRequests() > 50) {
            double evictionRate = (double) stats.getTotalEvictions() / stats.getTotalRequests();
            if (evictionRate > 0.3) {
                log.error("캐시 제거율 매우 높음: {:.2f}% - 메모리 부족 의심", evictionRate * 100);
            } else if (evictionRate > 0.2) {
                log.warn("캐시 제거율 높음: {:.2f}% - TTL 설정 검토 추천", evictionRate * 100);
            }
        }
    }

    /**
     * 전반적인 캐시 건강도 체크
     */
    private void checkCacheHealth(CacheStatistics stats) {
        String healthStatus = evaluateCacheEfficiency(stats);
        if ("CRITICAL".equals(healthStatus)) {
            log.error("캐시 상태 위험: 즉시 점검 필요");
        } else if ("POOR".equals(healthStatus)) {
            log.warn("캐시 성능 저하: 최적화 검토 추천");
        }
    }

    /**
     * 캐시 효율성 평가
     */
    private String evaluateCacheEfficiency(CacheStatistics stats) {
        if (stats.getTotalRequests() < 10) {
            return "INSUFFICIENT_DATA";
        }

        double hitRatio = stats.getHitRatio();
        double evictionRate = stats.getTotalRequests() > 0 ?
            (double) stats.getTotalEvictions() / stats.getTotalRequests() : 0;

        if (hitRatio < 0.3 || evictionRate > 0.3) {
            return "CRITICAL";
        } else if (hitRatio < 0.5 || evictionRate > 0.2) {
            return "POOR";
        } else if (hitRatio < 0.7 || evictionRate > 0.1) {
            return "FAIR";
        } else {
            return "EXCELLENT";
        }
    }

    /**
     * 최적화 제안 여부 판단
     */
    private boolean shouldSuggestOptimization(CacheStatistics stats) {
        return stats.getTotalRequests() > 100 &&
               (stats.getHitRatio() < 0.6 ||
                (double) stats.getTotalEvictions() / stats.getTotalRequests() > 0.15);
    }

    /**
     * 성능 최적화 제안
     */
    private void suggestOptimizations(CacheStatistics stats) {
        log.info("=== 캐시 최적화 제안 ===");

        if (stats.getHitRatio() < 0.5) {
            log.info("1. 캐시 키 생성 로직 검토 - 넓은 범위의 데이터에 대한 캐시 고려");
            log.info("2. TTL 설정 검토 - 너무 짧은 TTL로 인한 조기 만료 가능성");
        }

        double evictionRate = (double) stats.getTotalEvictions() / stats.getTotalRequests();
        if (evictionRate > 0.2) {
            log.info("3. 메모리 할당 증가 검토 - Redis maxmemory 설정 확인");
            log.info("4. 데이터 크기 최적화 - 처리 데이터 압축 고려");
        }

        if (stats.getTotalRequests() > 1000 && stats.getHitRatio() > 0.8) {
            log.info("5. TTL 연장 검토 - 우수한 히트율로 캐시 유지 시간 연장 가능");
        }
    }

    /**
     * 캐시 통계 데이터 클래스
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheStatistics {
        private long totalHits;
        private long totalMisses;
        private long totalEvictions;
        private long totalRequests;
        private double hitRatio;
    }
}