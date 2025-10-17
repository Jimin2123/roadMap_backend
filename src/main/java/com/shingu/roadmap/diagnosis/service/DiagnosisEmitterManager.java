package com.shingu.roadmap.diagnosis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 관리 서비스
 * 진단 과정의 실시간 진행 상황을 클라이언트에게 전송합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisEmitterManager {

    private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간
    private static final String EVENT_NAME = "diagnosis-progress";
    private static final int EMITTER_MAX_AGE_HOURS = 2; // 2시간 이상 된 emitter는 정리
    private static final int MAX_EMITTERS = 1000; // 최대 emitter 개수 (메모리 leak 방지)

    private final Map<Long, EmitterWrapper> emitters = new ConcurrentHashMap<>();
    private final Set<Long> warnedDiagnosisIds = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

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
     * - 최대 emitter 개수 제한 (MAX_EMITTERS)
     * - 제한 초과 시 가장 오래된 emitter 강제 정리
     * - 모든 lifecycle 이벤트에서 방어적으로 제거 처리
     *
     * @param diagnosisId 진단 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long diagnosisId) {
        log.info("[DiagnosisEmitterManager.createEmitter] ENTER - diagnosisId: {}, currentEmitters: {}",
            diagnosisId, emitters.size());
        long startTime = System.currentTimeMillis();

        // 최대 emitter 개수 확인 및 정리
        if (emitters.size() >= MAX_EMITTERS) {
            log.warn("[DiagnosisEmitterManager.createEmitter] Maximum emitters limit reached ({}). Forcing cleanup of oldest emitters.", MAX_EMITTERS);
            forceCleanupOldestEmitters(MAX_EMITTERS / 10); // 10% 정리
        }

        log.debug("[DiagnosisEmitterManager.createEmitter] Creating new SseEmitter with timeout: {}ms", DEFAULT_TIMEOUT);
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        EmitterWrapper wrapper = new EmitterWrapper(emitter);

        // 방어적 제거 액션 (모든 lifecycle 이벤트에서 실행)
        Runnable removeAction = () -> {
            wrapper.markCompleted();
            EmitterWrapper removed = emitters.remove(diagnosisId);
            if (removed != null) {
                log.debug("[DiagnosisEmitterManager.createEmitter] Emitter removed from map for diagnosisId: {}", diagnosisId);
            }
        };

        // 완료 시 맵에서 제거
        emitter.onCompletion(() -> {
            log.info("[DiagnosisEmitterManager.createEmitter] SSE emitter completed for diagnosisId: {}", diagnosisId);
            removeAction.run();
        });

        // 타임아웃 시 맵에서 제거
        emitter.onTimeout(() -> {
            log.warn("[DiagnosisEmitterManager.createEmitter] SSE emitter timed out for diagnosisId: {}", diagnosisId);
            removeAction.run();
        });

        // 에러 발생 시 맵에서 제거
        emitter.onError((e) -> {
            log.error("[DiagnosisEmitterManager.createEmitter] SSE emitter error for diagnosisId: {}", diagnosisId, e);
            removeAction.run();
        });

        // 맵에 등록
        emitters.put(diagnosisId, wrapper);
        long duration = System.currentTimeMillis() - startTime;
        log.info("[DiagnosisEmitterManager.createEmitter] EXIT - diagnosisId: {}, duration: {}ms, totalEmitters: {}",
                diagnosisId, duration, emitters.size());

        return emitter;
    }

    /**
     * 가장 오래된 emitter들을 강제로 정리합니다.
     * 메모리 leak 방지를 위한 안전장치입니다.
     *
     * @param count 정리할 emitter 개수
     */
    private void forceCleanupOldestEmitters(int count) {
        List<Map.Entry<Long, EmitterWrapper>> sortedEntries = emitters.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getCreatedAt()))
                .limit(count)
                .toList();

        log.info("Force cleaning up {} oldest emitters", sortedEntries.size());

        sortedEntries.forEach(entry -> {
            Long diagnosisId = entry.getKey();
            EmitterWrapper wrapper = entry.getValue();

            try {
                if (!wrapper.isCompleted()) {
                    wrapper.getEmitter().complete();
                    wrapper.markCompleted();
                }
                emitters.remove(diagnosisId);
                log.info("Force cleaned up emitter for diagnosisId: {}", diagnosisId);
            } catch (Exception e) {
                log.warn("Failed to force cleanup emitter for diagnosisId: {}", diagnosisId, e);
                // 실패하더라도 맵에서는 제거
                emitters.remove(diagnosisId);
            }
        });
    }

    /**
     * 진행 상황을 특정 진단 ID의 클라이언트에게 전송합니다.
     *
     * @param diagnosisId 진단 ID
     * @param progress 진행 상황 데이터
     */
    public void sendProgress(Long diagnosisId, DiagnosisProgressResponse progress) {
        EmitterWrapper wrapper = emitters.get(diagnosisId);
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
            emitters.remove(diagnosisId);
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
        EmitterWrapper wrapper = emitters.get(diagnosisId);
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
            emitters.remove(diagnosisId);
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
        EmitterWrapper wrapper = emitters.get(diagnosisId);
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
            emitters.remove(diagnosisId);
            warnedDiagnosisIds.remove(diagnosisId);
        }
    }

    /**
     * 주기적으로 오래된 emitter를 정리합니다.
     * 5분마다 실행되며, 2시간 이상 된 emitter를 제거합니다.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다 실행
    public void cleanupExpiredEmitters() {
        List<Long> expiredKeys = new ArrayList<>();

        emitters.forEach((diagnosisId, wrapper) -> {
            if (wrapper.isExpired() || wrapper.isCompleted()) {
                expiredKeys.add(diagnosisId);
            }
        });

        if (!expiredKeys.isEmpty()) {
            log.info("Cleaning up {} expired/completed emitters", expiredKeys.size());

            expiredKeys.forEach(diagnosisId -> {
                EmitterWrapper wrapper = emitters.remove(diagnosisId);
                if (wrapper != null && !wrapper.isCompleted()) {
                    try {
                        wrapper.getEmitter().complete();
                        wrapper.markCompleted();
                        log.info("Cleaned up expired emitter for diagnosisId: {}", diagnosisId);
                    } catch (Exception e) {
                        log.warn("Failed to complete emitter during cleanup for diagnosisId: {}", diagnosisId, e);
                    }
                }
                // 경고 추적도 함께 정리
                warnedDiagnosisIds.remove(diagnosisId);
            });
        }
    }

    /**
     * 특정 진단 ID의 emitter가 존재하는지 확인합니다.
     *
     * @param diagnosisId 진단 ID
     * @return emitter 존재 여부
     */
    public boolean hasEmitter(Long diagnosisId) {
        return emitters.containsKey(diagnosisId);
    }
}
