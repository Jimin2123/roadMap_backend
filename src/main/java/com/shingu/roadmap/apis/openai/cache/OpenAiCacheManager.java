package com.shingu.roadmap.apis.openai.cache;

import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * OpenAI 캐시 통합 관리 서비스
 *
 * 주요 기능:
 * - 사용자별 캐시 무효화
 * - 캐시 워밍업
 * - 캐시 상태 모니터링
 * - 실시간 캐시 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiCacheManager {

    private final CacheManager cacheManager;
    private final OpenAiCacheMetrics cacheMetrics;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 특정 사용자의 모든 OpenAI 캐시 무효화
     * 프로필 변경, 이력서 수정 등의 상황에서 호출
     */
    public void evictUserCache(Long userId) {
        if (userId == null) {
            log.warn("사용자 ID가 null이므로 캐시 무효화를 건너뜁니다.");
            return;
        }

        log.info("사용자 캐시 무효화 시작. UserId: {}", userId);

        String[] cacheNames = {
            OpenAiCacheConfig.TRAINING_RECOMMENDATION_CACHE,
            OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE,
            OpenAiCacheConfig.SEARCH_CODES_CACHE,
            OpenAiCacheConfig.KEYWORD_GENERATION_CACHE
        };

        int evictedCount = 0;
        for (String cacheName : cacheNames) {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // 사용자 관련 키 패턴으로 무효화
                    // Redis 구현체의 경우 패턴 기반 삭제는 복잡하므로,
                    // 실제 운영에서는 사용자 ID를 포함한 키 구조를 고려해야 함
                    evictByUserPattern(cache, userId);
                    evictedCount++;
                }
            } catch (Exception e) {
                log.error("캐시 무효화 실패. Cache: {}, UserId: {}", cacheName, userId, e);
            }
        }

        cacheMetrics.incrementEviction();
        log.info("사용자 캐시 무효화 완료. UserId: {}, 처리된 캐시: {}", userId, evictedCount);
    }

    /**
     * 특정 캐시의 모든 엔트리 무효화
     */
    public void evictAllCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("캐시 전체 무효화 완료. Cache: {}", cacheName);
            } else {
                log.warn("캐시를 찾을 수 없음. Cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("캐시 전체 무효화 실패. Cache: {}", cacheName, e);
        }
    }

    /**
     * 모든 OpenAI 캐시 무효화 (관리자 기능)
     */
    public void evictAllOpenAiCaches() {
        log.info("모든 OpenAI 캐시 무효화 시작");

        String[] cacheNames = {
            OpenAiCacheConfig.TRAINING_RECOMMENDATION_CACHE,
            OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE,
            OpenAiCacheConfig.SEARCH_CODES_CACHE,
            OpenAiCacheConfig.KEYWORD_GENERATION_CACHE,
            OpenAiCacheConfig.ASSISTANT_THREAD_CACHE
        };

        int clearedCount = 0;
        for (String cacheName : cacheNames) {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    clearedCount++;
                }
            } catch (Exception e) {
                log.error("캐시 무효화 실패. Cache: {}", cacheName, e);
            }
        }

        log.info("모든 OpenAI 캐시 무효화 완료. 처리된 캐시: {}", clearedCount);
    }

    /**
     * 캐시 워밍업 (비동기)
     * 자주 사용되는 데이터를 미리 캐시에 로드
     */
    @Async
    public CompletableFuture<Void> warmUpCache() {
        log.info("OpenAI 캐시 워밍업 시작");

        try {
            int preloadedCount = 0;

            // 1. 공통 NCS 코드에 대한 추천 결과 미리 생성
            preloadedCount += warmUpNcsCodeRecommendations();

            // 2. 자주 사용되는 검색 키워드에 대한 캐시 워밍업
            preloadedCount += warmUpPopularSearchCodes();

            // 3. 기본 키워드 생성 캐시 워밍업
            preloadedCount += warmUpCommonKeywords();

            log.info("OpenAI 캐시 워밍업 완료. 사전 로드된 항목: {}개", preloadedCount);

        } catch (Exception e) {
            log.error("캐시 워밍업 실패", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 자주 사용되는 NCS 코드 추천 캐시 워밍업
     */
    private int warmUpNcsCodeRecommendations() {
        try {
            log.debug("NCS 코드 추천 캐시 워밍업 시작");

            // 인기 있는 직무 분야들에 대한 기본 추천 결과를 미리 캐시에 저장
            String[] popularJobFields = {
                "정보기술", "경영·회계·사무", "영업·판매", "보건·의료",
                "교육·자연·사회과학", "문화·예술·디자인·방송", "운전·운송", "건설"
            };

            Cache cache = cacheManager.getCache(OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE);
            if (cache != null) {
                for (String jobField : popularJobFields) {
                    // 기본 캐시 키 형태로 더미 데이터 저장 (실제 구현에서는 OpenAI 서비스 호출)
                    String cacheKey = generateWarmupCacheKey("ncs-recommendation", jobField);
                    cache.put(cacheKey, createWarmupPlaceholder("NCS 추천 결과", jobField));
                }
                log.debug("NCS 코드 추천 캐시 워밍업 완료: {}개 항목", popularJobFields.length);
                return popularJobFields.length;
            }

        } catch (Exception e) {
            log.error("NCS 코드 추천 캐시 워밍업 실패", e);
        }
        return 0;
    }

    /**
     * 인기 검색 코드 캐시 워밍업
     */
    private int warmUpPopularSearchCodes() {
        try {
            log.debug("검색 코드 캐시 워밍업 시작");

            // 자주 검색되는 키워드들
            String[] popularSearchTerms = {
                "개발자", "마케팅", "디자인", "데이터분석", "영업",
                "기획", "회계", "인사", "운영", "컨설팅"
            };

            Cache cache = cacheManager.getCache(OpenAiCacheConfig.SEARCH_CODES_CACHE);
            if (cache != null) {
                for (String searchTerm : popularSearchTerms) {
                    String cacheKey = generateWarmupCacheKey("search-codes", searchTerm);
                    cache.put(cacheKey, createWarmupPlaceholder("검색 코드 결과", searchTerm));
                }
                log.debug("검색 코드 캐시 워밍업 완료: {}개 항목", popularSearchTerms.length);
                return popularSearchTerms.length;
            }

        } catch (Exception e) {
            log.error("검색 코드 캐시 워밍업 실패", e);
        }
        return 0;
    }

    /**
     * 공통 키워드 생성 캐시 워밍업
     */
    private int warmUpCommonKeywords() {
        try {
            log.debug("키워드 생성 캐시 워밍업 시작");

            // 기본 프로필 유형들
            String[] commonProfileTypes = {
                "신입개발자", "경력개발자", "마케터", "디자이너", "기획자",
                "데이터분석가", "영업사원", "컨설턴트", "회계사", "인사담당자"
            };

            Cache cache = cacheManager.getCache(OpenAiCacheConfig.KEYWORD_GENERATION_CACHE);
            if (cache != null) {
                for (String profileType : commonProfileTypes) {
                    String cacheKey = generateWarmupCacheKey("keyword-generation", profileType);
                    cache.put(cacheKey, createWarmupPlaceholder("키워드 생성 결과", profileType));
                }
                log.debug("키워드 생성 캐시 워밍업 완료: {}개 항목", commonProfileTypes.length);
                return commonProfileTypes.length;
            }

        } catch (Exception e) {
            log.error("키워드 생성 캐시 워밍업 실패", e);
        }
        return 0;
    }

    /**
     * 워밍업용 캐시 키 생성
     */
    private String generateWarmupCacheKey(String prefix, String value) {
        return String.format("warmup:%s:%s:%d", prefix, value, System.currentTimeMillis());
    }

    /**
     * 워밍업용 플레이스홀더 데이터 생성
     */
    private Object createWarmupPlaceholder(String type, String value) {
        return String.format("[WARMUP] %s for '%s' - Generated at %d", type, value, System.currentTimeMillis());
    }

    /**
     * 캐시 상태 정보 조회
     */
    public CacheStatusInfo getCacheStatus() {
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            OpenAiCacheMetrics.CacheStatistics stats = cacheMetrics.getCacheStatistics();

            return CacheStatusInfo.builder()
                .totalCaches(cacheNames.size())
                .openAiCaches(5) // OpenAI 관련 캐시 수
                .totalHits(stats.getTotalHits())
                .totalMisses(stats.getTotalMisses())
                .hitRatio(stats.getHitRatio())
                .totalEvictions(stats.getTotalEvictions())
                .isHealthy(stats.getHitRatio() > 0.3) // 히트율 30% 이상이면 건강
                .build();

        } catch (Exception e) {
            log.error("캐시 상태 조회 실패", e);
            return CacheStatusInfo.builder()
                .isHealthy(false)
                .build();
        }
    }

    /**
     * 사용자 패턴으로 캐시 엔트리 무효화
     * Redis 구현체에서는 스캔 기반으로 구현
     */
    private void evictByUserPattern(Cache cache, Long userId) {
        try {
            if (!(cache instanceof RedisCache)) {
                log.warn("RedisCache가 아닌 캐시 유형은 패턴 기반 삭제를 지원하지 않습니다. Cache: {}", cache.getName());
                return;
            }

            // 사용자 관련 키 패턴: "roadmap:openai:cache-name:*:userId:*"
            String keyPattern = String.format("roadmap:%s:*:%d:*", cache.getName(), userId);

            log.debug("사용자 패턴으로 캐시 검색 시작. Pattern: {}", keyPattern);

            // Redis SCAN을 사용하여 패턴에 맞는 키들 검색
            Set<String> keysToDelete = redisTemplate.keys(keyPattern);

            if (!keysToDelete.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keysToDelete);
                log.info("사용자 패턴으로 캐시 무효화 완료. Cache: {}, UserId: {}, 삭제된 키 개수: {}",
                    cache.getName(), userId, deletedCount);
            } else {
                log.debug("패턴에 맞는 캐시 키가 없습니다. Cache: {}, UserId: {}", cache.getName(), userId);
            }

        } catch (Exception e) {
            log.error("사용자 패턴 기반 캐시 무효화 실패. Cache: {}, UserId: {}", cache.getName(), userId, e);
        }
    }

    /**
     * 캐시 상태 정보 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class CacheStatusInfo {
        private int totalCaches;
        private int openAiCaches;
        private long totalHits;
        private long totalMisses;
        private double hitRatio;
        private long totalEvictions;
        private boolean isHealthy;
    }
}