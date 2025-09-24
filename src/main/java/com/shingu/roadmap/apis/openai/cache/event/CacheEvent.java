package com.shingu.roadmap.apis.openai.cache.event;

import org.springframework.context.ApplicationEvent;

/**
 * 캐시 이벤트의 기본 클래스
 */
public abstract class CacheEvent extends ApplicationEvent {

    private final String cacheName;
    private final Object cacheKey;
    private final String operation;

    protected CacheEvent(Object source, String cacheName, Object cacheKey, String operation) {
        super(source);
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
        this.operation = operation;
    }

    public String getCacheName() {
        return cacheName;
    }

    public Object getCacheKey() {
        return cacheKey;
    }

    public String getOperation() {
        return operation;
    }
}