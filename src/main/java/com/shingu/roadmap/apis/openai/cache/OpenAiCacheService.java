package com.shingu.roadmap.apis.openai.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiCacheService {

    private final OpenAiConfig config;
    private final SecureLogger secureLogger;
    private final ObjectMapper objectMapper;

    // L1 캐시: 메모리 기반 빠른 응답 캐시
    private final ConcurrentHashMap<String, CacheEntry> l1Cache = new ConcurrentHashMap<>();

    // L2 캐시: Redis 기반 지속성 캐시 (Spring Cache 사용)

    /**
     * 2단계 캐싱 전략으로 응답 조회
     * L1 (메모리) -> L2 (Redis) -> API 호출 순서
     */
    public Mono<String> getCachedResponse(String operation, String cacheKey,
                                        Supplier<Mono<String>> apiCall) {
        return getCachedResponse(operation, cacheKey, apiCall, true);
    }

    public Mono<String> getCachedResponse(String operation, String cacheKey,
                                        Supplier<Mono<String>> apiCall,
                                        boolean cacheable) {
        if (!cacheable || !isCacheable(operation, cacheKey)) {
            secureLogger.logCacheMiss(cacheKey, operation, "NOT_CACHEABLE");
            return apiCall.get();
        }

        // L1 캐시 확인
        CacheEntry l1Entry = l1Cache.get(cacheKey);
        if (l1Entry != null && !l1Entry.isExpired()) {
            secureLogger.logCacheHit(cacheKey, operation, "L1_MEMORY");
            updateAccessTime(cacheKey);
            return Mono.just(l1Entry.getValue());
        }

        // L2 캐시 확인 (Redis)
        return getFromL2Cache(cacheKey)
                .doOnNext(cachedValue -> {
                    secureLogger.logCacheHit(cacheKey, operation, "L2_REDIS");
                    // L2에서 찾은 값을 L1에도 저장
                    putToL1Cache(cacheKey, cachedValue);
                })
                .switchIfEmpty(
                    // 캐시 미스 - API 호출 후 캐시 저장
                    apiCall.get()
                            .doOnNext(response -> {
                                secureLogger.logCacheMiss(cacheKey, operation, "FULL_MISS");
                                putToL1Cache(cacheKey, response);
                                putToL2Cache(cacheKey, response);
                            })
                );
    }

    /**
     * L2 캐시에서 데이터 조회 (Redis/Spring Cache)
     */
    public Mono<String> getFromL2Cache(String cacheKey) {
        // 실제 구현에서는 Spring Cache나 Redis를 사용
        // 현재는 단순히 빈 값 반환 (캐시 미스)
        return Mono.empty();
    }

    /**
     * 캐시 키 생성 (해시 기반)
     */
    public String generateCacheKey(String operation, Object... params) {
        try {
            String paramsString = objectMapper.writeValueAsString(params);
            String hash = DigestUtils.md5DigestAsHex(paramsString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return String.format("openai:%s:%s", operation, hash);
        } catch (JsonProcessingException e) {
            // JSON 직렬화 실패 시 해시 기반으로 폴백
            int hash = Objects.hash(params);
            return String.format("openai:%s:%d", operation, hash);
        }
    }

    /**
     * 캐시 가능 여부 판단
     */
    public boolean isCacheable(String operation, String cacheKey) {
        // 실시간성이 중요한 작업은 캐시하지 않음
        if (operation.contains("realtime") || operation.contains("personalized")) {
            return false;
        }

        // 캐시 키가 너무 길면 캐시하지 않음
        if (cacheKey.length() > 250) {
            return false;
        }

        // 설정에서 캐싱이 비활성화된 경우
        return config.isMonitoringEnabled(); // 임시로 모니터링 설정 사용
    }

    /**
     * L1 캐시에 데이터 저장
     */
    private void putToL1Cache(String cacheKey, String value) {
        if (l1Cache.size() >= config.getMaxCacheEntries()) {
            evictOldestEntries();
        }

        CacheEntry entry = new CacheEntry(
                value,
                LocalDateTime.now().plus(Duration.ofMinutes(30)), // L1 TTL: 30분
                LocalDateTime.now()
        );

        l1Cache.put(cacheKey, entry);
        secureLogger.logCacheEvent("L1_PUT", cacheKey, value.length());
    }

    /**
     * L2 캐시에 데이터 저장 (Redis)
     */
    private void putToL2Cache(String cacheKey, String value) {
        // 실제 구현에서는 @CachePut 사용하거나 Redis Template 직접 사용
        secureLogger.logCacheEvent("L2_PUT", cacheKey, value.length());
    }

    /**
     * L1 캐시 접근 시간 업데이트
     */
    private void updateAccessTime(String cacheKey) {
        CacheEntry entry = l1Cache.get(cacheKey);
        if (entry != null) {
            entry.updateAccessTime();
        }
    }

    /**
     * L1 캐시에서 오래된 항목 제거 (LRU 방식)
     */
    private void evictOldestEntries() {
        int targetSize = (int) (config.getMaxCacheEntries() * 0.8); // 80%까지 축소

        l1Cache.entrySet().stream()
                .sorted((e1, e2) -> e1.getValue().getLastAccessTime()
                        .compareTo(e2.getValue().getLastAccessTime()))
                .limit(l1Cache.size() - targetSize)
                .forEach(entry -> {
                    l1Cache.remove(entry.getKey());
                    secureLogger.logCacheEvent("L1_EVICT", entry.getKey(), 0);
                });
    }

    /**
     * 캐시 통계 조회
     */
    public CacheStats getCacheStats() {
        return CacheStats.builder()
                .l1Size(l1Cache.size())
                .l1MaxSize(config.getMaxCacheEntries())
                .build();
    }

    /**
     * 캐시 무효화
     */
    public void invalidateCache(String operation) {
        String pattern = "openai:" + operation + ":";

        // L1 캐시 무효화
        l1Cache.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getKey().startsWith(pattern);
            if (shouldRemove) {
                secureLogger.logCacheEvent("L1_INVALIDATE", entry.getKey(), 0);
            }
            return shouldRemove;
        });

        // L2 캐시 무효화는 Spring Cache Evict 사용
        secureLogger.logCacheEvent("CACHE_INVALIDATE", operation, 0);
    }

    /**
     * 전체 캐시 클리어
     */
    public void clearAllCache() {
        int l1Size = l1Cache.size();
        l1Cache.clear();
        secureLogger.logCacheEvent("L1_CLEAR_ALL", "*", l1Size);
    }

    /**
     * 만료된 캐시 항목 정리 (스케줄링으로 주기적 호출)
     */
    public void cleanupExpiredEntries() {
        int removed = 0;
        var iterator = l1Cache.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
                secureLogger.logCacheEvent("L1_EXPIRE", entry.getKey(), 0);
            }
        }

        if (removed > 0) {
            log.debug("Cleaned up {} expired cache entries", removed);
        }
    }

    /**
     * 캐시 항목 클래스
     */
    private static class CacheEntry {
        private final String value;
        private final LocalDateTime expiryTime;
        private volatile LocalDateTime lastAccessTime;

        public CacheEntry(String value, LocalDateTime expiryTime, LocalDateTime lastAccessTime) {
            this.value = value;
            this.expiryTime = expiryTime;
            this.lastAccessTime = lastAccessTime;
        }

        public String getValue() { return value; }
        public LocalDateTime getLastAccessTime() { return lastAccessTime; }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }

        public void updateAccessTime() {
            this.lastAccessTime = LocalDateTime.now();
        }
    }

    /**
     * 캐시 통계 클래스
     */
    @Builder
    @Data
    public static class CacheStats {
        private int l1Size;
        private int l1MaxSize;
        private double l1HitRate;
        private double l2HitRate;
        private long totalRequests;
        private long cacheHits;
        private long cacheMisses;
    }

    private void logCacheEvent(String event, String key, int size) {
        secureLogger.logCacheEvent(event, key, size);
    }
}