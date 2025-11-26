package com.shingu.roadmap.diagnosis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SSE Emitter 관리 서비스
 * 진단 과정의 실시간 진행 상황을 클라이언트에게 전송합니다.
 *
 * 메모리 관리:
 * - Caffeine Cache를 사용하여 TTL 기반 자동 만료 (2시간)
 * - 최대 1000개 emitter 제한
 * - Removal listener를 통한 자동 cleanup
 */
@Service
@Slf4j
public class DiagnosisEmitterManager {

    private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간
    private static final String EVENT_NAME = "diagnosis-progress";
    private static final int EMITTER_MAX_AGE_HOURS = 2;
    private static final int MAX_EMITTERS = 1000;

    private final Cache<Long, EmitterWrapper> emitters;
    private final Set<Long> warnedDiagnosisIds = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    /**
     * Caffeine Cache를 설정하여 자동 메모리 관리를 수행합니다.
     */
    public DiagnosisEmitterManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.emitters = Caffeine.newBuilder()
                .expireAfterWrite(EMITTER_MAX_AGE_HOURS, TimeUnit.HOURS)  // TTL 기반 자동 만료
                .maximumSize(MAX_EMITTERS)  // 최대 크기 제한
                .removalListener((Long key, EmitterWrapper value, RemovalCause cause) -> {
                    // Emitter 제거 시 자동 cleanup
                    if (key != null && value != null) {
                        log.info("[DiagnosisEmitterManager] Emitter removed - diagnosisId: {}, cause: {}, isCompleted: {}",
                                key, cause, value.isCompleted());

                        if (!value.isCompleted()) {
                            try {
                                value.getEmitter().complete();
                                value.markCompleted();
                                log.debug("[DiagnosisEmitterManager] Emitter auto-completed during removal - diagnosisId: {}", key);
                            } catch (Exception e) {
                                log.warn("[DiagnosisEmitterManager] Failed to complete emitter during removal - diagnosisId: {}", key, e);
                            }
                        }

                        // 경고 추적도 함께 정리
                        warnedDiagnosisIds.remove(key);
                    }
                })
                .build();

        log.info("[DiagnosisEmitterManager] Initialized with Caffeine cache - TTL: {} hours, maxSize: {}",
                EMITTER_MAX_AGE_HOURS, MAX_EMITTERS);
    }

    /**
     * SseEmitter와 생성 시각을 함께 저장하는 래퍼 클래스
     */
    private static class EmitterWrapper {
        private final SseEmitter emitter;
        private final LocalDateTime createdAt;
        private boolean completed;

        EmitterWrapper(SseEmitter emitter) {
            this.emitter = emitter;
            this.createdAt = LocalDateTime.now();
            this.completed = false;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void markCompleted() {
            this.completed = true;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(createdAt.plusHours(EMITTER_MAX_AGE_HOURS));
        }
    }

    /**
     * 새로운 SSE Emitter를 생성하고 등록합니다.
     *
     * 메모리 leak 방지:
     * - Caffeine의 TTL 기반 자동 만료 (2시간)
     * - 최대 emitter 개수 자동 제한 (Caffeine의 maximumSize)
     * - Removal listener를 통한 자동 cleanup
     * - 모든 lifecycle 이벤트에서 방어적으로 제거 처리
     *
     * @param diagnosisId 진단 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long diagnosisId) {
        long currentSize = emitters.estimatedSize();
        log.info("[DiagnosisEmitterManager.createEmitter] ENTER - diagnosisId: {}, currentEmitters: {}",
            diagnosisId, currentSize);
        long startTime = System.currentTimeMillis();

        log.debug("[DiagnosisEmitterManager.createEmitter] Creating new SseEmitter with timeout: {}ms", DEFAULT_TIMEOUT);
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        EmitterWrapper wrapper = new EmitterWrapper(emitter);

        // 방어적 제거 액션 (모든 lifecycle 이벤트에서 실행)
        Runnable removeAction = () -> {
            wrapper.markCompleted();
            emitters.invalidate(diagnosisId);  // Cache API 사용
            log.debug("[DiagnosisEmitterManager] Emitter invalidated for diagnosisId: {}", diagnosisId);
        };

        // 완료 시 캐시에서 제거
        emitter.onCompletion(() -> {
            log.info("[DiagnosisEmitterManager] SSE emitter completed for diagnosisId: {}", diagnosisId);
            removeAction.run();
        });

        // 타임아웃 시 캐시에서 제거
        emitter.onTimeout(() -> {
            log.warn("[DiagnosisEmitterManager] SSE emitter timed out for diagnosisId: {}", diagnosisId);
            removeAction.run();
        });

        // 에러 발생 시 캐시에서 제거
        emitter.onError((e) -> {
            log.error("[DiagnosisEmitterManager] SSE emitter error for diagnosisId: {}", diagnosisId, e);
            removeAction.run();
        });

        // 캐시에 등록 (Caffeine이 자동으로 크기 및 TTL 관리)
        emitters.put(diagnosisId, wrapper);
        long duration = System.currentTimeMillis() - startTime;
        log.info("[DiagnosisEmitterManager.createEmitter] EXIT - diagnosisId: {}, duration: {}ms, totalEmitters: {}",
                diagnosisId, duration, emitters.estimatedSize());

        return emitter;
    }

    /**
     * 진행 상황을 특정 진단 ID의 클라이언트에게 전송합니다.
     *
     * @param diagnosisId 진단 ID
     * @param progress 진행 상황 데이터
     */
    public void sendProgress(Long diagnosisId, DiagnosisProgressResponse progress) {
        EmitterWrapper wrapper = emitters.getIfPresent(diagnosisId);
        if (wrapper == null) {
            // 첫 번째 경고는 INFO 레벨로, 이후는 DEBUG로 (중복 로그 방지)
            if (warnedDiagnosisIds.add(diagnosisId)) {
                log.info("No SSE emitter for diagnosisId: {} - proceeding without real-time updates", diagnosisId);
            } else {
                log.debug("Progress skipped for diagnosisId: {} (no emitter)", diagnosisId);
            }
            return;
        }

        SseEmitter emitter = wrapper.getEmitter();
        try {
            String json = objectMapper.writeValueAsString(progress);
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(json));
            log.debug("Progress sent for diagnosisId: {}, step: {}, percentage: {}",
                    diagnosisId, progress.currentStep(), progress.progressPercentage());

        } catch (IOException e) {
            log.error("Failed to send progress for diagnosisId: {}", diagnosisId, e);
            wrapper.markCompleted();
            emitters.invalidate(diagnosisId);  // Cache API 사용
            emitter.completeWithError(e);
        }
    }

    /**
     * 진단 완료를 알리고 emitter를 완료합니다.
     *
     * @param diagnosisId 진단 ID
     * @param finalProgress 최종 진행 상황 (완료 상태)
     */
    public void complete(Long diagnosisId, DiagnosisProgressResponse finalProgress) {
        EmitterWrapper wrapper = emitters.getIfPresent(diagnosisId);
        if (wrapper == null) {
            log.warn("No emitter found for diagnosisId: {} during completion", diagnosisId);
            return;
        }

        SseEmitter emitter = wrapper.getEmitter();
        try {
            // 최종 진행 상황 전송
            String json = objectMapper.writeValueAsString(finalProgress);
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(json));

            // emitter 완료
            emitter.complete();
            wrapper.markCompleted();
            log.info("Diagnosis completed and emitter closed for diagnosisId: {}", diagnosisId);

        } catch (IOException e) {
            log.error("Failed to complete emitter for diagnosisId: {}", diagnosisId, e);
            emitter.completeWithError(e);
            wrapper.markCompleted();
        } finally {
            emitters.invalidate(diagnosisId);  // Cache API 사용
            warnedDiagnosisIds.remove(diagnosisId);
        }
    }

    /**
     * 에러 발생 시 클라이언트에게 알리고 emitter를 종료합니다.
     *
     * @param diagnosisId 진단 ID
     * @param errorProgress 에러 상태의 진행 상황
     */
    public void completeWithError(Long diagnosisId, DiagnosisProgressResponse errorProgress) {
        EmitterWrapper wrapper = emitters.getIfPresent(diagnosisId);
        if (wrapper == null) {
            log.warn("No emitter found for diagnosisId: {} during error completion", diagnosisId);
            return;
        }

        SseEmitter emitter = wrapper.getEmitter();
        try {
            // 에러 상태 전송
            String json = objectMapper.writeValueAsString(errorProgress);
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(json));

            // emitter 에러로 완료
            emitter.completeWithError(new RuntimeException(errorProgress.currentMessage()));
            wrapper.markCompleted();
            log.info("Diagnosis failed and emitter closed for diagnosisId: {}", diagnosisId);

        } catch (IOException e) {
            log.error("Failed to send error for diagnosisId: {}", diagnosisId, e);
            emitter.completeWithError(e);
            wrapper.markCompleted();
        } finally {
            emitters.invalidate(diagnosisId);  // Cache API 사용
            warnedDiagnosisIds.remove(diagnosisId);
        }
    }


    /**
     * Caffeine Cache가 자동으로 TTL 기반 cleanup을 수행합니다.
     *
     * Note: @Scheduled cleanup 메서드는 더 이상 필요하지 않습니다.
     * Caffeine은 다음과 같이 자동으로 관리합니다:
     * - expireAfterWrite: 2시간 후 자동 만료
     * - maximumSize: 1000개 초과 시 LRU 방식으로 제거
     * - removalListener: 제거 시 자동 cleanup 수행
     */
}
