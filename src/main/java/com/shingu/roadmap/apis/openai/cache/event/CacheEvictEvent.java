package com.shingu.roadmap.apis.openai.cache.event;

/**
 * 캐시 제거 이벤트
 */
public class CacheEvictEvent extends CacheEvent {

    public CacheEvictEvent(Object source, String cacheName, Object cacheKey, String operation) {
        super(source, cacheName, cacheKey, operation);
    }
}