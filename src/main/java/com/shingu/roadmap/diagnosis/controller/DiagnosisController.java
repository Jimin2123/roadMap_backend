package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import com.shingu.roadmap.diagnosis.dto.request.DiagnosisStartRequest;
import com.shingu.roadmap.diagnosis.dto.request.JobConfirmationRequest;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.service.DiagnosisEmitterManager;
import com.shingu.roadmap.diagnosis.service.DiagnosisService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 진단 컨트롤러
 * 사용자의 역량 진단 및 직무 추천 API를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class DiagnosisController implements DiagnosisControllerSwagger {

    private final DiagnosisService diagnosisService;
    private final DiagnosisEmitterManager emitterManager;

    /**
     * 진단 실행
     * 사용자의 프로필을 기반으로 역량 진단을 시작합니다.
     * 비동기로 실행되며, SSE를 통해 진행 상황을 확인할 수 있습니다.
     */
    @Override
    @PostMapping("/api/v1/diagnosis")
    public ResponseEntity<DiagnosisProgressResponse> runDiagnosis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) DiagnosisStartRequest request
    ) {
        try {
            Long memberId = userDetails.getMemberId();
            log.info("Starting diagnosis for memberId: {}", memberId);

            // 새 진단 생성 및 ID 발급
            Long diagnosisId = diagnosisService.createNewDiagnosis(memberId);
            log.info("New diagnosisId created: {}", diagnosisId);

            // 진단 상태를 IN_PROGRESS로 변경
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS);

            // 비동기로 진단 실행
            diagnosisService.executeDiagnosisAsync(memberId, diagnosisId);

            // 즉시 응답 반환 (진행 상황은 SSE로 확인)
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .currentStep(DiagnosisStep.RESUME_ANALYSIS)
                    .status(DiagnosisStatus.IN_PROGRESS)
                    .currentMessage("진단이 시작되었습니다. SSE를 통해 진행 상황을 확인하세요.")
                    .progressPercentage(0)
                    .build();

            return ResponseEntity.accepted().body(progress);

        } catch (Exception e) {
            log.error("Failed to start diagnosis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 진단 과정 실시간 스트리밍 (SSE)
     * Server-Sent Events를 통해 진단 진행 상황을 실시간으로 전송합니다.
     */
    @Override
    @GetMapping("/api/v1/diagnosis/{id}/stream")
    public SseEmitter streamDiagnosisProgress(
            @PathVariable("id") Long diagnosisId
    ) {
        log.info("SSE connection requested for diagnosisId: {}", diagnosisId);

        // SSE Emitter 생성 및 등록
        SseEmitter emitter = emitterManager.createEmitter(diagnosisId);

        log.info("SSE emitter created for diagnosisId: {}", diagnosisId);
        return emitter;
    }

    /**
     * 최종 진단 결과 조회
     * DB에 저장된 진단 결과를 조회합니다.
     */
    @Override
    @GetMapping("/api/v1/diagnosis/result/{id}")
    public ResponseEntity<DiagnosisResultResponse> getFinalDiagnosisResult(
            @PathVariable("id") Long diagnosisId
    ) {
        try {
            log.info("Fetching diagnosis result for diagnosisId: {}", diagnosisId);

            DiagnosisResultResponse result = diagnosisService.findDiagnosisResult(diagnosisId);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("Diagnosis result not found for diagnosisId: {}", diagnosisId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Failed to fetch diagnosis result for diagnosisId: {}", diagnosisId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 직접 직무 선택 (Human-in-the-loop)
     * AI의 신뢰도가 낮아 사용자가 직접 직무를 선택하는 경우
     * 비동기로 진단을 재개하며, SSE를 통해 진행 상황을 확인할 수 있습니다.
     */
    @Override
    @PostMapping("/api/v1/diagnosis/{id}/job-confirmation")
    public ResponseEntity<DiagnosisProgressResponse> selectJobManually(
            @PathVariable("id") Long diagnosisId,
            @RequestBody JobConfirmationRequest request
    ) {
        try {
            log.info("User manually selected NCS code: {} for diagnosisId: {}",
                    request.selectedNcsCode(), diagnosisId);

            // 진단 상태를 AWAITING_USER_INPUT에서 IN_PROGRESS로 변경
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS);

            // 비동기로 진단 재개 (SSE로 진행 상황 전송)
            diagnosisService.continueWithUserSelectionAsync(diagnosisId, request.selectedNcsCode());

            // 즉시 응답 반환 (진행 상황은 SSE로 확인)
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .currentStep(DiagnosisStep.JOB_MATCHING)
                    .status(DiagnosisStatus.IN_PROGRESS)
                    .currentMessage("사용자 선택을 반영하여 진단을 재개합니다. SSE를 통해 진행 상황을 확인하세요.")
                    .progressPercentage(33)
                    .build();

            log.info("Diagnosis continuation initiated successfully with user selection");
            return ResponseEntity.accepted().body(progress);

        } catch (Exception e) {
            log.error("Failed to continue diagnosis with user selection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
