package com.shingu.roadmap.diagnosis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
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

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * 새로운 SSE Emitter를 생성하고 등록합니다.
     *
     * @param diagnosisId 진단 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long diagnosisId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        // 완료 시 맵에서 제거
        emitter.onCompletion(() -> {
            log.info("SSE emitter completed for diagnosisId: {}", diagnosisId);
            emitters.remove(diagnosisId);
        });

        // 타임아웃 시 맵에서 제거
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for diagnosisId: {}", diagnosisId);
            emitters.remove(diagnosisId);
        });

        // 에러 발생 시 맵에서 제거
        emitter.onError((e) -> {
            log.error("SSE emitter error for diagnosisId: {}", diagnosisId, e);
            emitters.remove(diagnosisId);
        });

        // 맵에 등록
        emitters.put(diagnosisId, emitter);
        log.info("SSE emitter created and registered for diagnosisId: {}", diagnosisId);

        return emitter;
    }

    /**
     * 진행 상황을 특정 진단 ID의 클라이언트에게 전송합니다.
     *
     * @param diagnosisId 진단 ID
     * @param progress 진행 상황 데이터
     */
    public void sendProgress(Long diagnosisId, DiagnosisProgressResponse progress) {
        SseEmitter emitter = emitters.get(diagnosisId);
        if (emitter == null) {
            log.warn("No emitter found for diagnosisId: {}", diagnosisId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(progress);
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(json));
            log.debug("Progress sent for diagnosisId: {}, step: {}, percentage: {}",
                    diagnosisId, progress.currentStep(), progress.progressPercentage());

        } catch (IOException e) {
            log.error("Failed to send progress for diagnosisId: {}", diagnosisId, e);
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
        SseEmitter emitter = emitters.get(diagnosisId);
        if (emitter == null) {
            log.warn("No emitter found for diagnosisId: {} during completion", diagnosisId);
            return;
        }

        try {
            // 최종 진행 상황 전송
            String json = objectMapper.writeValueAsString(finalProgress);
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(json));

            // emitter 완료
            emitter.complete();
            log.info("Diagnosis completed and emitter closed for diagnosisId: {}", diagnosisId);

        } catch (IOException e) {
            log.error("Failed to complete emitter for diagnosisId: {}", diagnosisId, e);
            emitter.completeWithError(e);
        } finally {
            emitters.remove(diagnosisId);
        }
    }

    /**
     * 에러 발생 시 클라이언트에게 알리고 emitter를 종료합니다.
     *
     * @param diagnosisId 진단 ID
     * @param errorProgress 에러 상태의 진행 상황
     */
    public void completeWithError(Long diagnosisId, DiagnosisProgressResponse errorProgress) {
        SseEmitter emitter = emitters.get(diagnosisId);
        if (emitter == null) {
            log.warn("No emitter found for diagnosisId: {} during error completion", diagnosisId);
            return;
        }

        try {
            // 에러 상태 전송
            String json = objectMapper.writeValueAsString(errorProgress);
            emitter.send(SseEmitter.event()
                    .name(EVENT_NAME)
                    .data(json));

            // emitter 에러로 완료
            emitter.completeWithError(new RuntimeException(errorProgress.currentMessage()));
            log.info("Diagnosis failed and emitter closed for diagnosisId: {}", diagnosisId);

        } catch (IOException e) {
            log.error("Failed to send error for diagnosisId: {}", diagnosisId, e);
            emitter.completeWithError(e);
        } finally {
            emitters.remove(diagnosisId);
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
