package com.shingu.roadmap.apis.openai.cache.event;

/**
 * 캐시 히트 이벤트
 */
public class CacheHitEvent extends CacheEvent {

    public CacheHitEvent(Object source, String cacheName, Object cacheKey, String operation) {
        super(source, cacheName, cacheKey, operation);
    }
}