package com.shingu.roadmap.apis.openai.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenAI 캐시 이벤트 처리 및 모니터링
 *
 * 주요 기능:
 * - 캐시 Hit/Miss 이벤트 처리
 * - 대용량 캐시 작업 모니터링
 * - 성능 메트릭 수집
 * - 리얼타임 알림 시스템
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCacheEventListener {

    private final SecureLogger secureLogger;
    private final OpenAiCacheMetrics cacheMetrics;
    private final ObjectMapper objectMapper;

    // 성능 모니터링을 위한 카운터
    private final AtomicLong consecutiveMisses = new AtomicLong(0);
    private final AtomicLong totalOperations = new AtomicLong(0);

    // 알림 임계값
    private static final long CONSECUTIVE_MISS_THRESHOLD = 10;
    private static final long LARGE_DATA_SIZE_THRESHOLD = 50000; // 50KB

    /**
     * 캐시 히트 이벤트 처리
     */
    public void logCacheHit(String cacheName, Object key, String operation) {
        if (isOpenAiCache(cacheName)) {
            try {
                String sessionKey = generateSessionKey();
                secureLogger.logCacheHit(sessionKey, operation, String.valueOf(key));
                cacheMetrics.incrementHit();

                // 연속 미스 카운터 리셋
                consecutiveMisses.set(0);
                totalOperations.incrementAndGet();

                log.debug("OpenAI Cache Hit - Cache: {}, Key: {}, Operation: {}, Total Ops: {}",
                    cacheName, maskKey(key), operation, totalOperations.get());

                // 주기적으로 성능 리포트
                reportPerformanceIfNeeded();

            } catch (Exception e) {
                log.error("캐시 히트 로깅 실패. Cache: {}", cacheName, e);
            }
        }
    }

    /**
     * 캐시 미스 이벤트 처리
     */
    public void logCacheMiss(String cacheName, Object key, String operation) {
        if (isOpenAiCache(cacheName)) {
            try {
                String sessionKey = generateSessionKey();
                secureLogger.logCacheMiss(sessionKey, operation, String.valueOf(key));
                cacheMetrics.incrementMiss();

                // 연속 미스 모니터링
                long misses = consecutiveMisses.incrementAndGet();
                totalOperations.incrementAndGet();

                log.debug("OpenAI Cache Miss - Cache: {}, Key: {}, Operation: {}, Consecutive Misses: {}",
                    cacheName, maskKey(key), operation, misses);

                // 연속 미스 알림
                if (misses >= CONSECUTIVE_MISS_THRESHOLD) {
                    log.warn("캐시 연속 미스 발생. Cache: {}, Consecutive Misses: {}, 성능 점검 필요",
                           cacheName, misses);
                }

                reportPerformanceIfNeeded();

            } catch (Exception e) {
                log.error("캐시 미스 로깅 실패. Cache: {}", cacheName, e);
            }
        }
    }

    /**
     * 캐시 저장 이벤트 처리
     */
    public void logCachePut(String cacheName, Object key, Object value, String operation) {
        if (isOpenAiCache(cacheName)) {
            try {
                String sessionKey = generateSessionKey();
                int dataSize = estimateDataSize(value);

                secureLogger.logCacheEvent("CACHE_PUT", String.valueOf(key), dataSize);

                // 대용량 데이터 알림
                if (dataSize > LARGE_DATA_SIZE_THRESHOLD) {
                    log.warn("대용량 데이터 캐시 저장. Cache: {}, Size: {} bytes, Key: {}",
                           cacheName, dataSize, maskKey(key));
                }

                log.debug("OpenAI Cache Put - Cache: {}, Key: {}, Operation: {}, Size: {} bytes",
                    cacheName, maskKey(key), operation, dataSize);

            } catch (Exception e) {
                log.error("캐시 Put 로깅 실패. Cache: {}", cacheName, e);
            }
        }
    }

    /**
     * 캐시 제거 이벤트 처리
     */
    public void logCacheEvict(String cacheName, Object key, String operation) {
        if (isOpenAiCache(cacheName)) {
            try {
                String sessionKey = generateSessionKey();
                secureLogger.logCacheEvent("CACHE_EVICT", String.valueOf(key), 0);
                cacheMetrics.incrementEviction();

                log.debug("OpenAI Cache Evict - Cache: {}, Key: {}, Operation: {}",
                    cacheName, maskKey(key), operation);

                // 비정상적인 대량 제거 감지
                checkEvictionPattern(cacheName);

            } catch (Exception e) {
                log.error("캐시 Evict 로깅 실패. Cache: {}", cacheName, e);
            }
        }
    }

    /**
     * OpenAI 캐시인지 확인
     */
    private boolean isOpenAiCache(String cacheName) {
        if (cacheName == null) {
            return false;
        }
        return cacheName.startsWith("openai:");
    }

    /**
     * 캐시 키에서 작업 타입 추출
     * 버전 정보를 활용한 더 안정적인 operation 추출
     */
    private String extractOperationFromKey(Object key) {
        if (key == null) {
            return "unknown";
        }

        String keyStr = key.toString();

        // 버전 정보 다음의 메서드명을 직접 추출하여 더 안정적으로 처리
        try {
            // 형식: v1.2:methodName:... 에서 methodName 추출
            String[] parts = keyStr.split(":", 3);
            if (parts.length >= 2) {
                String methodName = parts[1];
                // 메서드명이 비어있지 않다면 직접 반환
                if (!methodName.isEmpty() && !methodName.equals("safe") && !methodName.equals("emergency")) {
                    return methodName;
                }
            }
        } catch (Exception e) {
            log.debug("메서드명 직접 추출 실패, fallback 방식 사용: {}", e.getMessage());
        }

        // Fallback: 기존 문자열 매칭 방식 (안전장치)
        if (keyStr.contains("recommendTrainingCourse")) {
            return "recommendTrainingCourse";
        } else if (keyStr.contains("recommendNcsCodeUsingAssistant")) {
            return "recommendNcsCodeUsingAssistant";
        } else if (keyStr.contains("recommendSearchCodes")) {
            return "recommendSearchCodes";
        } else if (keyStr.contains("generateKeyword")) {
            return "generateKeyword";
        } else if (keyStr.contains("recommendDesiredJobCodeUsingAssistant")) {
            return "recommendDesiredJobCodeUsingAssistant";
        }

        return "unknown";
    }

    /**
     * 민감한 캐시 키 마스킹
     */
    private String maskKey(Object key) {
        if (key == null) {
            return "null";
        }

        String keyStr = key.toString();
        if (keyStr.length() <= 8) {
            return "****";
        }

        // 앞 4자리 + 마스킹 + 뒤 4자리
        return keyStr.substring(0, 4) + "****" + keyStr.substring(keyStr.length() - 4);
    }

    /**
     * 데이터 크기 추정 (성능 최적화를 위해 JSON 직렬화만 사용)
     */
    private int estimateDataSize(Object value) {
        if (value == null) {
            return 0;
        }

        try {
            // JSON 직렬화를 이용한 크기 추정 (가장 일관성 있고 성능이 좋음)
            String json = objectMapper.writeValueAsString(value);
            return json.getBytes().length;
        } catch (Exception e) {
            // JSON 직렬화 실패 시 toString() 기반 추정 사용 (Java 직렬화 제거로 성능 향상)
            log.debug("JSON 크기 추정 실패, toString() 기반 추정 사용: {}", e.getMessage());
            return value.toString().getBytes().length;
        }
    }


    /**
     * 성능 리포트 (주기적으로 호출)
     */
    private void reportPerformanceIfNeeded() {
        long totalOps = totalOperations.get();
        if (totalOps > 0 && totalOps % 100 == 0) {
            OpenAiCacheMetrics.CacheStatistics stats = cacheMetrics.getCacheStatistics();
            log.info("캐시 성능 리포트 - Total Ops: {}, Hit Ratio: {:.2f}%, Consecutive Misses: {}",
                   totalOps, stats.getHitRatio() * 100, consecutiveMisses.get());
        }
    }

    /**
     * 비정상적인 제거 패턴 감지
     */
    private void checkEvictionPattern(String cacheName) {
        OpenAiCacheMetrics.CacheStatistics stats = cacheMetrics.getCacheStatistics();
        long totalRequests = stats.getTotalRequests();
        long evictions = stats.getTotalEvictions();

        // 제거율이 20% 이상이면 경고
        if (totalRequests > 50 && (double) evictions / totalRequests > 0.2) {
            log.warn("높은 캐시 제거율 감지. Cache: {}, Eviction Rate: {:.2f}%",
                   cacheName, ((double) evictions / totalRequests) * 100);
        }
    }

    /**
     * 세션 키 생성 (타임스탬프 + 스레드 ID)
     */
    private String generateSessionKey() {
        return "cache_event_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 캐시 상태 리셋 (수동 호출용)
     */
    public void resetCounters() {
        consecutiveMisses.set(0);
        totalOperations.set(0);
        log.info("캐시 이벤트 카운터 리셋 완료");
    }
}