package com.shingu.roadmap.apis.openai.config;

import com.shingu.roadmap.apis.openai.cache.EventPublishingCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI API 전용 캐시 설정
 *
 * 특징:
 * - 개별 캐시별 최적화된 TTL 설정
 * - Redis 기반 분산 캐시 지원
 * - 메모리 효율적인 직렬화
 * - 실시간 모니터링 지원
 */
@Configuration
@EnableCaching
@EnableScheduling
public class OpenAiCacheConfig {

    public static final String TRAINING_RECOMMENDATION_CACHE = "openai:training-recommendation";
    public static final String NCS_CODE_RECOMMENDATION_CACHE = "openai:ncs-code-recommendation";
    public static final String SEARCH_CODES_CACHE = "openai:search-codes";
    public static final String KEYWORD_GENERATION_CACHE = "openai:keyword-generation";
    public static final String ASSISTANT_THREAD_CACHE = "openai:assistant-thread";

    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(1)) // 기본 TTL: 1시간
                // 캐시 키 구조: roadmap:openai:CACHE_NAME::KEY
                // prefixCacheNameWith: 캐시 이름에 "openai:" 접두사 추가 (openai:training-recommendation)
                // computePrefixWith: 실제 Redis 키에 "roadmap:" 접두사 추가 (roadmap:openai:training-recommendation::key)
                .prefixCacheNameWith("openai:") // 캐시 이름 프리픽스로 네임스페이스 분리
                .computePrefixWith(cacheName -> "roadmap:" + cacheName + ":"); // 키 프리픽스 설정

        // OpenAI 전용 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 훈련과정 추천 - 6시간 캐싱 (사용자 프로필 변경이 빈번하지 않음)
        // 비용이 높은 AI API 호출이므로 상대적으로 긴 TTL 설정
        cacheConfigurations.put(TRAINING_RECOMMENDATION_CACHE,
            defaultCacheConfig.entryTtl(Duration.ofHours(6)));

        // NCS 코드 추천 - 12시간 캐싱 (사용자 경력 정보는 자주 변경되지 않음)
        // AI 분석 결과이므로 안정적이고 재사용 가능
        cacheConfigurations.put(NCS_CODE_RECOMMENDATION_CACHE,
            defaultCacheConfig.entryTtl(Duration.ofHours(12)));

        // 검색 코드 추천 - 24시간 캐싱 (프로필 기반이므로 장기 캐싱 가능)
        // 상대적으로 정적인 매핑 정보
        cacheConfigurations.put(SEARCH_CODES_CACHE,
            defaultCacheConfig.entryTtl(Duration.ofHours(24)));

        // 키워드 생성 - 8시간 캐싱 (트렌드 반영을 위해 다소 짧게)
        // 키워드 트렌드 변화를 고려한 적절한 TTL
        cacheConfigurations.put(KEYWORD_GENERATION_CACHE,
            defaultCacheConfig.entryTtl(Duration.ofHours(8)));

        // Assistant Thread ID - 1시간 캐싱 (세션 기반이므로 짧게)
        // 사용자 세션과 연관된 일시적 데이터
        cacheConfigurations.put(ASSISTANT_THREAD_CACHE,
            defaultCacheConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 이벤트를 발행하는 기본 CacheManager
     * OpenAI 캐시 작업을 모니터링하고 이벤트를 자동으로 발행합니다.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
            CacheManager redisCacheManager,
            ApplicationEventPublisher eventPublisher) {
        return new EventPublishingCacheManager(redisCacheManager, eventPublisher);
    }
}