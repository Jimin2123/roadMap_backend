package com.shingu.roadmap.diagnosis.service;

import com.shingu.roadmap.diagnosis.domain.DiagnosisResult;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import com.shingu.roadmap.diagnosis.dto.internal.MemberWithProfile;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.exception.DiagnosisAccessDeniedException;
import com.shingu.roadmap.diagnosis.exception.DiagnosisAlreadyInProgressException;
import com.shingu.roadmap.diagnosis.exception.DiagnosisNotFoundException;
import com.shingu.roadmap.diagnosis.repository.DiagnosisResultRepository;
import com.shingu.roadmap.diagnosis.service.pipeline.CompetencyAnalysisProcessor;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisContext;
import com.shingu.roadmap.diagnosis.service.pipeline.DiagnosisProcessor;
import com.shingu.roadmap.diagnosis.service.pipeline.NcsRecommendationProcessor;
import com.shingu.roadmap.diagnosis.service.pipeline.ReportGenerationProcessor;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 진단 서비스 - 파이프라인 오케스트레이터
 *
 * 역할:
 * - 전체 진단 흐름을 총괄하고 각 프로세서를 순차적으로 실행
 * - 진단 컨텍스트를 생성하고 프로세서 간 데이터 전달 관리
 * - 오류 처리 및 진단 상태 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisService {

    private final DiagnosisResultRepository diagnosisResultRepository;
    private final NcsRecommendationProcessor ncsRecommendationProcessor;
    private final CompetencyAnalysisProcessor competencyAnalysisProcessor;
    private final ReportGenerationProcessor reportGenerationProcessor;
    private final DiagnosisEmitterManager emitterManager;
    private final DiagnosisStateService diagnosisStateService; // 분리된 서비스 주입

    // 프로세서별 진행률 및 메시지 매핑
    private static final Map<String, ProcessorProgress> PROCESSOR_PROGRESS_MAP = Map.of(
            "NcsRecommendationProcessor", new ProcessorProgress(DiagnosisStep.NCS_CODE_SUGGESTION, 33, "NCS 직무 코드를 추천하고 있습니다..."),
            "CompetencyAnalysisProcessor", new ProcessorProgress(DiagnosisStep.JOB_MATCHING, 66, "역량을 분석하고 있습니다..."),
            "ReportGenerationProcessor", new ProcessorProgress(DiagnosisStep.FINAL_REPORT, 90, "최종 보고서를 생성하고 있습니다...")
    );

    private record ProcessorProgress(DiagnosisStep step, int percentage, String message) {}

    /**
     * 비동기로 진단을 실행하고 SSE로 진행 상황을 전송합니다.
     *
     * @param memberId 진단 대상 회원 ID
     * @param diagnosisId 진단 ID (SSE 식별자)
     */
    @Async("diagnosisTaskExecutor")
    public void executeDiagnosisAsync(Long memberId, Long diagnosisId) {
        log.info("Starting async diagnosis for memberId: {}, diagnosisId: {}", memberId, diagnosisId);

        try {
            // 초기 상태 전송
            sendProgress(diagnosisId, DiagnosisStep.RESUME_ANALYSIS, 0, DiagnosisStatus.IN_PROGRESS, "진단을 시작합니다...");

            // 1. 사용자 프로필 및 모든 연관 데이터 조회 (Transactional 메서드 호출 - Fully Detached)
            MemberWithProfile memberData = diagnosisStateService.loadDiagnosisDataDetached(memberId);
            Member member = memberData.member();
            Profile profile = memberData.profile();

            // 2. 진단 컨텍스트 초기화 (진행 상황 콜백 포함)
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(memberId)
                    .diagnosisId(diagnosisId)
                    .profile(profile)
                    .success(true)
                    .progressCallback(progress -> emitterManager.sendProgress(diagnosisId, progress))
                    .build();

            // 3. 파이프라인 프로세서 목록
            List<DiagnosisProcessor> processors = List.of(
                    ncsRecommendationProcessor,
                    competencyAnalysisProcessor,
                    reportGenerationProcessor
            );

            // 4. 파이프라인 실행 (진행 상황 전송 포함)
            context = executePipelineWithProgress(processors, context);

            // 5. 결과 확인 및 완료 처리
            if (!context.isSuccess()) {
                log.error("Diagnosis failed for diagnosisId: {}, error: {}", diagnosisId, context.getErrorMessage());

                // 진단 실패 상태 DB에 저장
                diagnosisStateService.failDiagnosisWithError(diagnosisId, context.getErrorMessage());

                DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                        .diagnosisId(diagnosisId)
                        .status(DiagnosisStatus.FAILED)
                        .currentStep(DiagnosisStep.FINAL_REPORT)
                        .progressPercentage(100)
                        .currentMessage("진단 중 오류가 발생했습니다: " + context.getErrorMessage())
                        .build();
                emitterManager.completeWithError(diagnosisId, errorProgress);
                return;
            }

            // 6. 진단 결과 DB에 저장 (Transactional 메서드 호출)
            DiagnosisResultResponse diagnosisResultResponse = context.getDiagnosisResultResponse();
            if (diagnosisResultResponse != null) {
                diagnosisStateService.saveDiagnosisResultData(diagnosisId, diagnosisResultResponse);
            }

            // 7. 완료 상태 전송
            log.info("Diagnosis completed successfully for diagnosisId: {}", diagnosisId);
            DiagnosisProgressResponse finalProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.COMPLETED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단이 완료되었습니다.")
                    .build();
            emitterManager.complete(diagnosisId, finalProgress);

        } catch (Exception e) {
            log.error("Unexpected error during async diagnosis for diagnosisId: {}", diagnosisId, e);

            // 진단 실패 상태 DB에 저장
            diagnosisStateService.failDiagnosisWithError(diagnosisId, "진단 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());

            DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.FAILED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단 중 예기치 않은 오류가 발생했습니다.")
                    .build();
            emitterManager.completeWithError(diagnosisId, errorProgress);
        }
    }

    /**
     * 파이프라인 프로세서 순차 실행 (진행 상황 전송 포함)
     *
     * @param processors 프로세서 목록
     * @param context 진단 컨텍스트
     * @return 처리된 진단 컨텍스트
     */
    private DiagnosisContext executePipelineWithProgress(List<DiagnosisProcessor> processors, DiagnosisContext context) {
        for (DiagnosisProcessor processor : processors) {
            String processorName = processor.getName();
            log.info("Executing processor: {}", processorName);

            // 프로세서 시작 전 진행 상황 전송
            ProcessorProgress progress = PROCESSOR_PROGRESS_MAP.get(processorName);
            if (progress != null && context.getProgressCallback() != null) {
                sendProgress(context.getDiagnosisId(), progress.step(), progress.percentage(),
                        DiagnosisStatus.IN_PROGRESS, progress.message());
            }

            try {
                context = processor.process(context);

                if (!context.isSuccess()) {
                    log.error("Processor {} failed: {}", processorName, context.getErrorMessage());
                    break;
                }

                log.info("Processor {} completed successfully", processorName);

            } catch (Exception e) {
                log.error("Processor {} threw exception: {}", processorName, e.getMessage(), e);
                context.setSuccess(false);
                context.setErrorMessage("프로세서 실행 중 오류 발생: " + processorName);
                break;
            }
        }

        return context;
    }

    /**
     * 진행 상황을 SSE로 전송합니다.
     */
    private void sendProgress(Long diagnosisId, DiagnosisStep step, int percentage, DiagnosisStatus status, String message) {
        DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                .diagnosisId(diagnosisId)
                .currentStep(step)
                .progressPercentage(percentage)
                .status(status)
                .currentMessage(message)
                .build();
        emitterManager.sendProgress(diagnosisId, progress);
    }

    /**
     * 새로운 진단을 생성하고 진단 ID를 반환합니다.
     * 이미 진행 중인 진단이 있으면 예외를 발생시킵니다.
     *
     * TOCTOU 경쟁 조건 방지:
     * - 비관적 락을 사용하여 동시 요청 시 데이터베이스 레벨에서 직렬화
     * - 진단 생성 직후 즉시 IN_PROGRESS 상태로 전환하여 중복 생성 방지
     *
     * @param memberId 회원 ID
     * @return 생성된 진단 ID
     * @throws DiagnosisAlreadyInProgressException 이미 진행 중인 진단이 있는 경우
     */
    @Transactional
    public Long createNewDiagnosis(Long memberId) {
        log.info("Creating new diagnosis for memberId: {}", memberId);

        // 비관적 락으로 진행 중인 진단 확인 (TOCTOU 경쟁 조건 방지)
        List<DiagnosisStatus> inProgressStatuses = List.of(
                DiagnosisStatus.IN_PROGRESS,
                DiagnosisStatus.PENDING,
                DiagnosisStatus.AWAITING_USER_INPUT
        );

        Optional<DiagnosisResult> existingDiagnosis = diagnosisResultRepository
                .findInProgressDiagnosisWithLock(memberId, inProgressStatuses);

        if (existingDiagnosis.isPresent()) {
            DiagnosisResult existing = existingDiagnosis.get();
            log.warn("Diagnosis already in progress for memberId: {}, diagnosisId: {}, status: {}",
                    memberId, existing.getId(), existing.getStatus());
            throw new DiagnosisAlreadyInProgressException(memberId);
        }

        // 진단 생성 및 즉시 IN_PROGRESS 상태로 전환
        DiagnosisResult diagnosis = DiagnosisResult.createPending(memberId);
        DiagnosisResult saved = diagnosisResultRepository.save(diagnosis);

        // 즉시 IN_PROGRESS로 전환하여 경쟁 조건 최소화
        saved.startDiagnosis();
        diagnosisResultRepository.save(saved);

        log.info("New diagnosis created and started with diagnosisId: {}, status: {}",
                saved.getId(), saved.getStatus());

        return saved.getId();
    }

    /**
     * 진단 ID로 진단 결과를 조회합니다.
     *
     * @param diagnosisId 진단 ID
     * @param memberId 조회하는 회원 ID
     * @return 진단 결과 응답 DTO
     */
    @Transactional(readOnly = true)
    public DiagnosisResultResponse findDiagnosisResult(Long diagnosisId, Long memberId) {
        log.info("Finding diagnosis result for diagnosisId: {}, memberId: {}", diagnosisId, memberId);

        DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                .orElseThrow(() -> new DiagnosisNotFoundException(diagnosisId));

        // 소유권 확인
        verifyDiagnosisOwnership(diagnosisResult, memberId);

        // 삭제된 결과인 경우 예외 발생
        if (diagnosisResult.isDeleted()) {
            throw new DiagnosisNotFoundException("삭제된 진단 결과입니다. diagnosisId: " + diagnosisId);
        }

        // 완료되지 않은 진단인 경우
        if (!diagnosisResult.isCompleted()) {
            throw new IllegalStateException(
                    String.format("완료되지 않은 진단입니다. diagnosisId: %d, status: %s",
                            diagnosisId, diagnosisResult.getStatus())
            );
        }

        // DiagnosisResultData를 DiagnosisResultResponse로 변환
        com.shingu.roadmap.diagnosis.domain.DiagnosisResultData resultData = diagnosisResult.getResultData();

        DiagnosisResultResponse response = DiagnosisResultResponse.builder()
                .diagnosisId(diagnosisId)
                .summary(resultData.getSummary())
                .ncsAnalyses(resultData.getNcsAnalyses())
                .confidenceScore(resultData.getConfidenceScore())
                .radarChartData(resultData.getRadarChartData())
                .build();

        log.info("Diagnosis result found successfully for diagnosisId: {}", diagnosisId);
        return response;
    }

    /**
     * 진단 상태를 업데이트합니다.
     *
     * @param diagnosisId 진단 ID
     * @param status 진단 상태
     */
    @Transactional
    public void updateDiagnosisStatus(Long diagnosisId, DiagnosisStatus status) {
        log.info("Updating diagnosis status for diagnosisId: {} to {}", diagnosisId, status);

        DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                .orElseThrow(() -> new IllegalArgumentException("진단 정보를 찾을 수 없습니다. diagnosisId: " + diagnosisId));

        // 상태 전이 메서드 사용 (도메인 로직 활용)
        switch (status) {
            case IN_PROGRESS -> {
                if (diagnosisResult.getStatus() == DiagnosisStatus.PENDING) {
                    diagnosisResult.startDiagnosis();
                } else if (diagnosisResult.getStatus() == DiagnosisStatus.AWAITING_USER_INPUT) {
                    diagnosisResult.resumeDiagnosis();
                }
            }
            case AWAITING_USER_INPUT -> diagnosisResult.awaitUserInput();
            case FAILED -> diagnosisResult.failDiagnosis("Manual status update");
            default -> log.warn("Unexpected status transition to {} for diagnosisId: {}", status, diagnosisId);
        }

        diagnosisResultRepository.save(diagnosisResult);
    }

    /**
     * 사용자 선택을 반영하여 진단을 비동기로 계속 진행합니다. (SSE 지원)
     *
     * @param diagnosisId 진단 ID
     * @param selectedNcsCode 사용자가 선택한 NCS 코드
     */
    @Async("diagnosisTaskExecutor")
    public void continueWithUserSelectionAsync(Long diagnosisId, String selectedNcsCode) {
        log.info("Continuing diagnosis asynchronously for diagnosisId: {} with user selected NCS code: {}",
                diagnosisId, selectedNcsCode);

        try {
            // 진단 ID로 회원 ID 조회 및 상태 업데이트 (Transactional)
            Long memberId = diagnosisStateService.resumeDiagnosisAfterUserInput(diagnosisId);

            // 초기 상태 전송
            sendProgress(diagnosisId, DiagnosisStep.JOB_MATCHING, 33, DiagnosisStatus.IN_PROGRESS,
                    "사용자 선택을 반영하여 진단을 계속합니다...");

            // 1. 사용자 프로필 및 모든 연관 데이터 조회 (Transactional 메서드 호출 - Fully Detached)
            MemberWithProfile memberData = diagnosisStateService.loadDiagnosisDataDetached(memberId);
            Member member = memberData.member();
            Profile profile = memberData.profile();

            // 2. 진단 컨텍스트 초기화 (1단계는 건너뛰고 2단계부터 시작)
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(memberId)
                    .diagnosisId(diagnosisId)
                    .profile(profile)
                    .userSelectedNcsCode(selectedNcsCode)
                    .success(true)
                    .progressCallback(progress -> emitterManager.sendProgress(diagnosisId, progress))
                    .build();

            // 3. 2단계부터 파이프라인 실행
            List<DiagnosisProcessor> processors = List.of(
                    competencyAnalysisProcessor,
                    reportGenerationProcessor
            );

            // 4. 파이프라인 실행 (진행 상황 전송 포함)
            context = executePipelineWithProgress(processors, context);

            // 5. 결과 확인 및 완료 처리
            if (!context.isSuccess()) {
                log.error("Diagnosis continuation failed for diagnosisId: {}, error: {}",
                        diagnosisId, context.getErrorMessage());

                // 진단 실패 상태 DB에 저장
                diagnosisStateService.failDiagnosisWithError(diagnosisId, context.getErrorMessage());

                DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                        .diagnosisId(diagnosisId)
                        .status(DiagnosisStatus.FAILED)
                        .currentStep(DiagnosisStep.FINAL_REPORT)
                        .progressPercentage(100)
                        .currentMessage("진단 중 오류가 발생했습니다: " + context.getErrorMessage())
                        .build();
                emitterManager.completeWithError(diagnosisId, errorProgress);
                return;
            }

            // 6. 진단 결과 DB에 저장 (Transactional 메서드 호출)
            DiagnosisResultResponse diagnosisResultResponse = context.getDiagnosisResultResponse();
            if (diagnosisResultResponse != null) {
                diagnosisStateService.saveDiagnosisResultData(diagnosisId, diagnosisResultResponse);
            }

            // 7. 완료 상태 전송
            log.info("Diagnosis continued and completed successfully for diagnosisId: {}", diagnosisId);
            DiagnosisProgressResponse finalProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.COMPLETED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단이 완료되었습니다.")
                    .build();
            emitterManager.complete(diagnosisId, finalProgress);

        } catch (Exception e) {
            log.error("Unexpected error during async diagnosis continuation for diagnosisId: {}", diagnosisId, e);

            // 진단 실패 상태 DB에 저장
            diagnosisStateService.failDiagnosisWithError(diagnosisId, "진단 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());

            DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.FAILED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단 중 예기치 않은 오류가 발생했습니다.")
                    .build();
            emitterManager.completeWithError(diagnosisId, errorProgress);
        }
    }

    /**
     * 진단 ID로 소유권을 확인합니다. (Public 메서드, 컨트롤러에서 사용)
     *
     * @param diagnosisId 진단 ID
     * @param memberId 조회하는 회원 ID
     * @throws DiagnosisNotFoundException 진단을 찾을 수 없는 경우
     * @throws DiagnosisAccessDeniedException 소유권이 없는 경우
     */
    @Transactional(readOnly = true)
    public void verifyDiagnosisOwnershipById(Long diagnosisId, Long memberId) {
        DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                .orElseThrow(() -> new DiagnosisNotFoundException(diagnosisId));

        verifyDiagnosisOwnership(diagnosisResult, memberId);
    }

    /**
     * 진단 결과의 소유권을 확인합니다. (Private 헬퍼 메서드)
     *
     * @param diagnosisResult 진단 결과 엔티티
     * @param memberId 조회하는 회원 ID
     * @throws DiagnosisAccessDeniedException 소유권이 없는 경우
     */
    private void verifyDiagnosisOwnership(DiagnosisResult diagnosisResult, Long memberId) {
        if (!diagnosisResult.getMemberId().equals(memberId)) {
            log.warn("Diagnosis access denied. diagnosisId: {}, requestedBy: {}, owner: {}",
                    diagnosisResult.getId(), memberId, diagnosisResult.getMemberId());
            throw new DiagnosisAccessDeniedException(diagnosisResult.getId(), memberId);
        }
    }
}
