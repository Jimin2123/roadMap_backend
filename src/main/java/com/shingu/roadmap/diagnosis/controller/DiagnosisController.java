package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.diagnosis.dto.request.DiagnosisStartRequest;
import com.shingu.roadmap.diagnosis.dto.request.JobConfirmationRequest;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.service.DiagnosisService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 진단 컨트롤러
 * 사용자의 역량 진단 및 직무 추천 API를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class DiagnosisController implements DiagnosisControllerSwagger {

    private final DiagnosisService diagnosisService;

    /**
     * 진단 실행
     * 사용자의 프로필을 기반으로 역량 진단을 시작합니다.
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

            // 전체 진단 실행
            DiagnosisResultResponse result = diagnosisService.executeDiagnosis(memberId);

            // DiagnosisProgressResponse로 변환
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(result.diagnosisId())
                    .currentStep(com.shingu.roadmap.diagnosis.domain.DiagnosisStep.FINAL_REPORT)
                    .status(com.shingu.roadmap.diagnosis.domain.DiagnosisStatus.COMPLETED)
                    .currentMessage("진단이 완료되었습니다.")
                    .progressPercentage(100)
                    .build();

            return ResponseEntity.ok(progress);

        } catch (Exception e) {
            log.error("Failed to run diagnosis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 진단 과정 실시간 스트리밍 (SSE)
     * TODO: SSE 구현 필요
     */
    @Override
    @GetMapping("/api/v1/diagnosis/{id}/stream")
    public ResponseEntity<DiagnosisProgressResponse> streamDiagnosisProgress(
            @PathVariable("id") Long diagnosisId
    ) {
        // TODO: SSE 구현 필요
        log.warn("SSE streaming not implemented yet for diagnosisId: {}", diagnosisId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * 최종 진단 결과 조회
     */
    @Override
    @GetMapping("/api/v1/diagnosis/result/{id}")
    public ResponseEntity<DiagnosisResultResponse> getFinalDiagnosisResult(
            @PathVariable("id") Long diagnosisId
    ) {
        // TODO: 진단 결과를 DB에 저장하고 조회하는 기능 구현 필요
        // 현재는 임시로 NOT_IMPLEMENTED 반환
        log.warn("Get diagnosis result not fully implemented yet for diagnosisId: {}", diagnosisId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * 사용자 직접 직무 선택
     * AI의 신뢰도가 낮아 사용자가 직접 직무를 선택하는 경우
     */
    @Override
    @PostMapping("/api/v1/diagnosis/{id}/job-confirmation")
    public ResponseEntity<Void> selectJobManually(
            @PathVariable("id") Long diagnosisId,
            @RequestBody JobConfirmationRequest request
    ) {
        try {
            log.info("User manually selected NCS code: {} for diagnosisId: {}",
                    request.selectedNcsCode(), diagnosisId);

            // TODO: diagnosisId를 memberId로 매핑하는 로직 필요
            // 현재는 diagnosisId를 memberId로 사용 (임시)
            Long memberId = diagnosisId;

            // 사용자 선택 기반으로 진단 계속 진행
            DiagnosisResultResponse result = diagnosisService.continueWithUserSelection(
                    memberId,
                    request.selectedNcsCode()
            );

            log.info("Diagnosis continued successfully with user selection");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to continue diagnosis with user selection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
