package com.shingu.roadmap.apis.openai.cache.event;

/**
 * 캐시 저장 이벤트
 */
public class CachePutEvent extends CacheEvent {

    private final Object value;

    public CachePutEvent(Object source, String cacheName, Object cacheKey, String operation, Object value) {
        super(source, cacheName, cacheKey, operation);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}