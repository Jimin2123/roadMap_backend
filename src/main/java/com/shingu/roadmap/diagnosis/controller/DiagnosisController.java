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
import jakarta.validation.Valid;
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
        log.debug("[DiagnosisController.runDiagnosis] ENTER - userDetails: {}, request: {}",
            userDetails != null ? userDetails.getUsername() : "null", request);

        long startTime = System.currentTimeMillis();

        try {
            Long memberId = userDetails.getMemberId();
            log.info("[DiagnosisController.runDiagnosis] Starting diagnosis for memberId: {}", memberId);
            log.debug("[DiagnosisController.runDiagnosis] User authentication validated - memberId: {}", memberId);

            // 새 진단 생성 및 ID 발급
            log.debug("[DiagnosisController.runDiagnosis] Creating new diagnosis for memberId: {}", memberId);
            Long diagnosisId = diagnosisService.createNewDiagnosis(memberId);
            log.info("[DiagnosisController.runDiagnosis] New diagnosisId created: {} for memberId: {}",
                diagnosisId, memberId);

            // 진단 상태를 IN_PROGRESS로 변경
            log.debug("[DiagnosisController.runDiagnosis] Updating diagnosis status to IN_PROGRESS for diagnosisId: {}",
                diagnosisId);
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS);

            // 비동기로 진단 실행
            log.info("[DiagnosisController.runDiagnosis] Triggering async diagnosis execution - diagnosisId: {}, memberId: {}",
                diagnosisId, memberId);
            diagnosisService.executeDiagnosisAsync(memberId, diagnosisId);

            // 즉시 응답 반환 (진행 상황은 SSE로 확인)
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .currentStep(DiagnosisStep.RESUME_ANALYSIS)
                    .status(DiagnosisStatus.IN_PROGRESS)
                    .currentMessage("진단이 시작되었습니다. SSE를 통해 진행 상황을 확인하세요.")
                    .progressPercentage(0)
                    .build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisController.runDiagnosis] EXIT - diagnosisId: {}, duration: {}ms",
                diagnosisId, duration);
            log.debug("[DiagnosisController.runDiagnosis] Response constructed - diagnosisId: {}, status: {}, step: {}",
                diagnosisId, progress.status(), progress.currentStep());

            return ResponseEntity.accepted().body(progress);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisController.runDiagnosis] EXCEPTION - memberId: {}, duration: {}ms, error: {}",
                userDetails != null ? userDetails.getMemberId() : "null", duration, e.getMessage(), e);
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long diagnosisId
    ) {
        log.debug("[DiagnosisController.streamDiagnosisProgress] ENTER - diagnosisId: {}, userDetails: {}",
            diagnosisId, userDetails != null ? userDetails.getUsername() : "null");

        long startTime = System.currentTimeMillis();

        try {
            Long memberId = userDetails.getMemberId();
            log.info("[DiagnosisController.streamDiagnosisProgress] SSE connection requested - diagnosisId: {}, memberId: {}",
                diagnosisId, memberId);
            log.debug("[DiagnosisController.streamDiagnosisProgress] User authentication validated - memberId: {}", memberId);

            // 소유권 확인 (진단이 해당 사용자 것인지 검증)
            log.debug("[DiagnosisController.streamDiagnosisProgress] Verifying diagnosis ownership - diagnosisId: {}, memberId: {}",
                diagnosisId, memberId);
            diagnosisService.verifyDiagnosisOwnershipById(diagnosisId, memberId);
            log.debug("[DiagnosisController.streamDiagnosisProgress] Ownership verified successfully");

            // SSE Emitter 생성 및 등록
            log.debug("[DiagnosisController.streamDiagnosisProgress] Creating SSE emitter for diagnosisId: {}", diagnosisId);
            SseEmitter emitter = emitterManager.createEmitter(diagnosisId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisController.streamDiagnosisProgress] EXIT - diagnosisId: {}, duration: {}ms, emitter created",
                diagnosisId, duration);
            log.debug("[DiagnosisController.streamDiagnosisProgress] SSE emitter timeout: {}ms",
                emitter.getTimeout() != null ? emitter.getTimeout() : "default");

            return emitter;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisController.streamDiagnosisProgress] EXCEPTION - diagnosisId: {}, memberId: {}, duration: {}ms, error: {}",
                diagnosisId, userDetails != null ? userDetails.getMemberId() : "null", duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 최종 진단 결과 조회
     * DB에 저장된 진단 결과를 조회합니다.
     */
    @Override
    @GetMapping("/api/v1/diagnosis/result/{id}")
    public ResponseEntity<DiagnosisResultResponse> getFinalDiagnosisResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long diagnosisId
    ) {
        log.debug("[DiagnosisController.getFinalDiagnosisResult] ENTER - diagnosisId: {}, userDetails: {}",
            diagnosisId, userDetails != null ? userDetails.getUsername() : "null");

        long startTime = System.currentTimeMillis();

        try {
            Long memberId = userDetails.getMemberId();
            log.info("[DiagnosisController.getFinalDiagnosisResult] Fetching result - diagnosisId: {}, memberId: {}",
                diagnosisId, memberId);
            log.debug("[DiagnosisController.getFinalDiagnosisResult] User authentication validated - memberId: {}", memberId);

            log.debug("[DiagnosisController.getFinalDiagnosisResult] Calling service to find diagnosis result");
            DiagnosisResultResponse result = diagnosisService.findDiagnosisResult(diagnosisId, memberId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisController.getFinalDiagnosisResult] EXIT - diagnosisId: {}, duration: {}ms, result found",
                diagnosisId, duration);
            log.debug("[DiagnosisController.getFinalDiagnosisResult] Result summary - confidenceScore: {}, ncsAnalysesCount: {}",
                result.confidenceScore(), result.ncsAnalyses() != null ? result.ncsAnalyses().size() : 0);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisController.getFinalDiagnosisResult] EXCEPTION - diagnosisId: {}, memberId: {}, duration: {}ms, error: {}",
                diagnosisId, userDetails != null ? userDetails.getMemberId() : "null", duration, e.getMessage(), e);
            throw e;
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long diagnosisId,
            @RequestBody @Valid JobConfirmationRequest request
    ) {
        log.debug("[DiagnosisController.selectJobManually] ENTER - diagnosisId: {}, userDetails: {}, selectedNcsCode: {}",
            diagnosisId, userDetails != null ? userDetails.getUsername() : "null", request.selectedNcsCode());

        long startTime = System.currentTimeMillis();

        try {
            Long memberId = userDetails.getMemberId();
            log.info("[DiagnosisController.selectJobManually] User selection received - diagnosisId: {}, memberId: {}, selectedNcsCode: {}",
                diagnosisId, memberId, request.selectedNcsCode());
            log.debug("[DiagnosisController.selectJobManually] User authentication validated - memberId: {}", memberId);
            log.debug("[DiagnosisController.selectJobManually] Request validated - selectedNcsCode: {}", request.selectedNcsCode());

            // 소유권 확인
            log.debug("[DiagnosisController.selectJobManually] Verifying diagnosis ownership - diagnosisId: {}, memberId: {}",
                diagnosisId, memberId);
            diagnosisService.verifyDiagnosisOwnershipById(diagnosisId, memberId);
            log.debug("[DiagnosisController.selectJobManually] Ownership verified successfully");

            // 진단 상태를 AWAITING_USER_INPUT에서 IN_PROGRESS로 변경
            log.debug("[DiagnosisController.selectJobManually] Updating diagnosis status - diagnosisId: {}, from: AWAITING_USER_INPUT, to: IN_PROGRESS",
                diagnosisId);
            diagnosisService.updateDiagnosisStatus(diagnosisId, DiagnosisStatus.IN_PROGRESS);

            // 비동기로 진단 재개 (SSE로 진행 상황 전송)
            log.info("[DiagnosisController.selectJobManually] Triggering async diagnosis continuation - diagnosisId: {}, selectedNcsCode: {}",
                diagnosisId, request.selectedNcsCode());
            diagnosisService.continueWithUserSelectionAsync(diagnosisId, request.selectedNcsCode());

            // 즉시 응답 반환 (진행 상황은 SSE로 확인)
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .currentStep(DiagnosisStep.JOB_MATCHING)
                    .status(DiagnosisStatus.IN_PROGRESS)
                    .currentMessage("사용자 선택을 반영하여 진단을 재개합니다. SSE를 통해 진행 상황을 확인하세요.")
                    .progressPercentage(33)
                    .build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisController.selectJobManually] EXIT - diagnosisId: {}, duration: {}ms, continuation initiated",
                diagnosisId, duration);
            log.debug("[DiagnosisController.selectJobManually] Response constructed - diagnosisId: {}, status: {}, step: {}",
                diagnosisId, progress.status(), progress.currentStep());

            return ResponseEntity.accepted().body(progress);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisController.selectJobManually] EXCEPTION - diagnosisId: {}, memberId: {}, selectedNcsCode: {}, duration: {}ms, error: {}",
                diagnosisId, userDetails != null ? userDetails.getMemberId() : "null",
                request.selectedNcsCode(), duration, e.getMessage(), e);
            throw e;
        }
    }
}
