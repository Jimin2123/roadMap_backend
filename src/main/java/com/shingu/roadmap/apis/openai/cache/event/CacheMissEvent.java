package com.shingu.roadmap.apis.openai.cache.event;

/**
 * 캐시 미스 이벤트
 */
public class CacheMissEvent extends CacheEvent {

    public CacheMissEvent(Object source, String cacheName, Object cacheKey, String operation) {
        super(source, cacheName, cacheKey, operation);
    }
}