package com.shingu.roadmap.apis.openai.cache;

import com.shingu.roadmap.apis.openai.cache.event.CacheHitEvent;
import com.shingu.roadmap.apis.openai.cache.event.CacheMissEvent;
import com.shingu.roadmap.apis.openai.cache.event.CachePutEvent;
import com.shingu.roadmap.apis.openai.cache.event.CacheEvictEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 이벤트를 발행하는 CacheManager 래퍼
 *
 * 실제 CacheManager를 감싸서 캐시 작업 시 이벤트를 자동으로 발행합니다.
 * Spring의 기본 캐시 메커니즘을 그대로 유지하면서 모니터링 기능을 추가합니다.
 */
@Slf4j
@Component("eventPublishingCacheManager")
@RequiredArgsConstructor
public class EventPublishingCacheManager implements CacheManager {

    private final CacheManager delegateCacheManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Cache getCache(String name) {
        Cache cache = delegateCacheManager.getCache(name);
        if (cache != null && isOpenAiCache(name)) {
            return new EventPublishingCache(cache, eventPublisher, name);
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegateCacheManager.getCacheNames();
    }

    private boolean isOpenAiCache(String cacheName) {
        return cacheName != null && cacheName.startsWith("openai:");
    }

    /**
     * 이벤트를 발행하는 Cache 래퍼
     */
    private static class EventPublishingCache implements Cache {

        private final Cache delegateCache;
        private final ApplicationEventPublisher eventPublisher;
        private final String cacheName;

        public EventPublishingCache(Cache delegateCache, ApplicationEventPublisher eventPublisher, String cacheName) {
            this.delegateCache = delegateCache;
            this.eventPublisher = eventPublisher;
            this.cacheName = cacheName;
        }

        @Override
        public String getName() {
            return delegateCache.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegateCache.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper result = delegateCache.get(key);

            try {
                if (result != null) {
                    // 캐시 히트
                    eventPublisher.publishEvent(new CacheHitEvent(this, cacheName, key, extractOperationFromStack()));
                } else {
                    // 캐시 미스
                    eventPublisher.publishEvent(new CacheMissEvent(this, cacheName, key, extractOperationFromStack()));
                }
            } catch (Exception e) {
                log.debug("캐시 이벤트 발행 실패: {}", e.getMessage());
            }

            return result;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            T result = delegateCache.get(key, type);

            try {
                if (result != null) {
                    eventPublisher.publishEvent(new CacheHitEvent(this, cacheName, key, extractOperationFromStack()));
                } else {
                    eventPublisher.publishEvent(new CacheMissEvent(this, cacheName, key, extractOperationFromStack()));
                }
            } catch (Exception e) {
                log.debug("캐시 이벤트 발행 실패: {}", e.getMessage());
            }

            return result;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            boolean existedBefore = delegateCache.get(key) != null;
            T result = delegateCache.get(key, valueLoader);

            try {
                if (existedBefore) {
                    eventPublisher.publishEvent(new CacheHitEvent(this, cacheName, key, extractOperationFromStack()));
                } else {
                    eventPublisher.publishEvent(new CacheMissEvent(this, cacheName, key, extractOperationFromStack()));
                    if (result != null) {
                        eventPublisher.publishEvent(new CachePutEvent(this, cacheName, key, extractOperationFromStack(), result));
                    }
                }
            } catch (Exception e) {
                log.debug("캐시 이벤트 발행 실패: {}", e.getMessage());
            }

            return result;
        }

        @Override
        public void put(Object key, Object value) {
            delegateCache.put(key, value);

            try {
                eventPublisher.publishEvent(new CachePutEvent(this, cacheName, key, extractOperationFromStack(), value));
            } catch (Exception e) {
                log.debug("캐시 Put 이벤트 발행 실패: {}", e.getMessage());
            }
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            ValueWrapper result = delegateCache.putIfAbsent(key, value);

            try {
                if (result == null) {
                    // 새로 추가됨
                    eventPublisher.publishEvent(new CachePutEvent(this, cacheName, key, extractOperationFromStack(), value));
                }
                // 기존 값이 있었다면 히트로 간주
                else {
                    eventPublisher.publishEvent(new CacheHitEvent(this, cacheName, key, extractOperationFromStack()));
                }
            } catch (Exception e) {
                log.debug("캐시 putIfAbsent 이벤트 발행 실패: {}", e.getMessage());
            }

            return result;
        }

        @Override
        public void evict(Object key) {
            delegateCache.evict(key);

            try {
                eventPublisher.publishEvent(new CacheEvictEvent(this, cacheName, key, extractOperationFromStack()));
            } catch (Exception e) {
                log.debug("캐시 Evict 이벤트 발행 실패: {}", e.getMessage());
            }
        }

        @Override
        public void clear() {
            delegateCache.clear();

            try {
                eventPublisher.publishEvent(new CacheEvictEvent(this, cacheName, "ALL_KEYS", extractOperationFromStack()));
            } catch (Exception e) {
                log.debug("캐시 Clear 이벤트 발행 실패: {}", e.getMessage());
            }
        }

        /**
         * 스택 트레이스에서 호출한 메서드명 추출
         */
        private String extractOperationFromStack() {
            try {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

                for (StackTraceElement element : stackTrace) {
                    String className = element.getClassName();
                    String methodName = element.getMethodName();

                    // OpenAI 서비스의 메서드 찾기
                    if (className.contains("OpenAiService") || className.contains("openai")) {
                        return methodName;
                    }
                }

                // fallback: 캐시 관련이 아닌 첫 번째 애플리케이션 메서드
                for (StackTraceElement element : stackTrace) {
                    String className = element.getClassName();
                    if (className.startsWith("com.shingu.roadmap") &&
                        !className.contains("Cache") &&
                        !className.contains("$")) {
                        return element.getMethodName();
                    }
                }
            } catch (Exception e) {
                log.debug("스택에서 메서드명 추출 실패: {}", e.getMessage());
            }

            return "unknown";
        }
    }
}