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
        log.info("[DiagnosisService.executeDiagnosisAsync] ENTER - memberId: {}, diagnosisId: {}", memberId, diagnosisId);
        long totalStartTime = System.currentTimeMillis();

        try {
            // 초기 상태 전송
            log.debug("[DiagnosisService.executeDiagnosisAsync] Sending initial progress");
            sendProgress(diagnosisId, DiagnosisStep.RESUME_ANALYSIS, 0, DiagnosisStatus.IN_PROGRESS, "진단을 시작합니다...");

            // 1. 사용자 프로필 및 모든 연관 데이터 조회 (Transactional 메서드 호출 - Fully Detached)
            log.debug("[DiagnosisService.executeDiagnosisAsync] Loading member data - memberId: {}", memberId);
            long loadStartTime = System.currentTimeMillis();
            MemberWithProfile memberData = diagnosisStateService.loadDiagnosisDataDetached(memberId);
            Member member = memberData.member();
            Profile profile = memberData.profile();
            long loadDuration = System.currentTimeMillis() - loadStartTime;

            log.info("[DiagnosisService.executeDiagnosisAsync] Member data loaded in {}ms - skillCount: {}, projectCount: {}",
                loadDuration,
                profile.getProfileSkills() != null ? profile.getProfileSkills().size() : 0,
                profile.getResume() != null && profile.getResume().getProjects() != null ?
                    profile.getResume().getProjects().size() : 0);

            // 2. 진단 컨텍스트 초기화 (진행 상황 콜백 포함)
            log.debug("[DiagnosisService.executeDiagnosisAsync] Initializing diagnosis context");
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(memberId)
                    .diagnosisId(diagnosisId)
                    .profile(profile)
                    .success(true)
                    .progressCallback(progress -> emitterManager.sendProgress(diagnosisId, progress))
                    .build();
            log.debug("[DiagnosisService.executeDiagnosisAsync] Context initialized successfully");

            // 3. 파이프라인 프로세서 목록
            List<DiagnosisProcessor> processors = List.of(
                    ncsRecommendationProcessor,
                    competencyAnalysisProcessor,
                    reportGenerationProcessor
            );
            log.info("[DiagnosisService.executeDiagnosisAsync] Starting pipeline with {} processors", processors.size());

            // 4. 파이프라인 실행 (진행 상황 전송 포함)
            long pipelineStartTime = System.currentTimeMillis();
            context = executePipelineWithProgress(processors, context);
            long pipelineDuration = System.currentTimeMillis() - pipelineStartTime;
            log.info("[DiagnosisService.executeDiagnosisAsync] Pipeline execution completed in {}ms - success: {}",
                pipelineDuration, context.isSuccess());

            // 5. 결과 확인 및 완료 처리
            if (!context.isSuccess()) {
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.error("[DiagnosisService.executeDiagnosisAsync] Diagnosis FAILED - diagnosisId: {}, totalDuration: {}ms, error: {}",
                    diagnosisId, totalDuration, context.getErrorMessage());

                // 진단 실패 상태 DB에 저장
                log.debug("[DiagnosisService.executeDiagnosisAsync] Saving failure state to DB");
                diagnosisStateService.failDiagnosisWithError(diagnosisId, context.getErrorMessage());

                DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                        .diagnosisId(diagnosisId)
                        .status(DiagnosisStatus.FAILED)
                        .currentStep(DiagnosisStep.FINAL_REPORT)
                        .progressPercentage(100)
                        .currentMessage("진단 중 오류가 발생했습니다: " + context.getErrorMessage())
                        .build();
                log.debug("[DiagnosisService.executeDiagnosisAsync] Sending error completion via SSE");
                emitterManager.completeWithError(diagnosisId, errorProgress);
                log.info("[DiagnosisService.executeDiagnosisAsync] EXIT (FAILED) - diagnosisId: {}, totalDuration: {}ms",
                    diagnosisId, totalDuration);
                return;
            }

            // 6. 진단 결과 DB에 저장 (Transactional 메서드 호출)
            DiagnosisResultResponse diagnosisResultResponse = context.getDiagnosisResultResponse();
            if (diagnosisResultResponse != null) {
                log.debug("[DiagnosisService.executeDiagnosisAsync] Saving diagnosis result to DB - diagnosisId: {}", diagnosisId);
                long saveStartTime = System.currentTimeMillis();
                diagnosisStateService.saveDiagnosisResultData(diagnosisId, diagnosisResultResponse);
                long saveDuration = System.currentTimeMillis() - saveStartTime;
                log.info("[DiagnosisService.executeDiagnosisAsync] Result saved to DB in {}ms", saveDuration);
            } else {
                log.warn("[DiagnosisService.executeDiagnosisAsync] No diagnosis result response to save - diagnosisId: {}", diagnosisId);
            }

            // 7. 완료 상태 전송
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.info("[DiagnosisService.executeDiagnosisAsync] Diagnosis COMPLETED successfully - diagnosisId: {}, totalDuration: {}ms",
                diagnosisId, totalDuration);
            DiagnosisProgressResponse finalProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.COMPLETED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단이 완료되었습니다.")
                    .build();
            log.debug("[DiagnosisService.executeDiagnosisAsync] Sending completion via SSE");
            emitterManager.complete(diagnosisId, finalProgress);
            log.info("[DiagnosisService.executeDiagnosisAsync] EXIT (SUCCESS) - diagnosisId: {}, totalDuration: {}ms",
                diagnosisId, totalDuration);

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.error("[DiagnosisService.executeDiagnosisAsync] EXCEPTION - diagnosisId: {}, memberId: {}, totalDuration: {}ms, error: {}",
                diagnosisId, memberId, totalDuration, e.getMessage(), e);

            // 진단 실패 상태 DB에 저장
            log.debug("[DiagnosisService.executeDiagnosisAsync] Saving unexpected error state to DB");
            diagnosisStateService.failDiagnosisWithError(diagnosisId, "진단 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());

            DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.FAILED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단 중 예기치 않은 오류가 발생했습니다.")
                    .build();
            log.debug("[DiagnosisService.executeDiagnosisAsync] Sending exception completion via SSE");
            emitterManager.completeWithError(diagnosisId, errorProgress);
            log.info("[DiagnosisService.executeDiagnosisAsync] EXIT (EXCEPTION) - diagnosisId: {}, totalDuration: {}ms",
                diagnosisId, totalDuration);
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
        log.debug("[DiagnosisService.executePipelineWithProgress] ENTER - processorCount: {}, diagnosisId: {}",
            processors.size(), context.getDiagnosisId());
        long totalPipelineStartTime = System.currentTimeMillis();
        int processedCount = 0;

        for (DiagnosisProcessor processor : processors) {
            String processorName = processor.getName();
            processedCount++;
            log.info("[DiagnosisService.executePipelineWithProgress] Executing processor {}/{} - name: {}",
                processedCount, processors.size(), processorName);
            long processorStartTime = System.currentTimeMillis();

            // 프로세서 시작 전 진행 상황 전송
            ProcessorProgress progress = PROCESSOR_PROGRESS_MAP.get(processorName);
            if (progress != null && context.getProgressCallback() != null) {
                log.debug("[DiagnosisService.executePipelineWithProgress] Sending progress for processor: {} - step: {}, percentage: {}%",
                    processorName, progress.step(), progress.percentage());
                sendProgress(context.getDiagnosisId(), progress.step(), progress.percentage(),
                        DiagnosisStatus.IN_PROGRESS, progress.message());
            } else {
                log.warn("[DiagnosisService.executePipelineWithProgress] No progress mapping found for processor: {}", processorName);
            }

            try {
                log.debug("[DiagnosisService.executePipelineWithProgress] Starting processor execution: {}", processorName);
                context = processor.process(context);
                long processorDuration = System.currentTimeMillis() - processorStartTime;

                if (!context.isSuccess()) {
                    log.error("[DiagnosisService.executePipelineWithProgress] Processor FAILED - name: {}, duration: {}ms, error: {}",
                        processorName, processorDuration, context.getErrorMessage());
                    log.debug("[DiagnosisService.executePipelineWithProgress] Breaking pipeline execution at processor {}/{}",
                        processedCount, processors.size());
                    break;
                }

                log.info("[DiagnosisService.executePipelineWithProgress] Processor completed successfully - name: {}, duration: {}ms",
                    processorName, processorDuration);

            } catch (Exception e) {
                long processorDuration = System.currentTimeMillis() - processorStartTime;
                log.error("[DiagnosisService.executePipelineWithProgress] Processor EXCEPTION - name: {}, duration: {}ms, error: {}",
                    processorName, processorDuration, e.getMessage(), e);
                context.setSuccess(false);
                context.setErrorMessage("프로세서 실행 중 오류 발생: " + processorName);
                log.debug("[DiagnosisService.executePipelineWithProgress] Breaking pipeline execution due to exception at processor {}/{}",
                    processedCount, processors.size());
                break;
            }
        }

        long totalPipelineDuration = System.currentTimeMillis() - totalPipelineStartTime;
        log.info("[DiagnosisService.executePipelineWithProgress] EXIT - totalDuration: {}ms, processedCount: {}/{}, success: {}",
            totalPipelineDuration, processedCount, processors.size(), context.isSuccess());

        return context;
    }

    /**
     * 진행 상황을 SSE로 전송합니다.
     */
    private void sendProgress(Long diagnosisId, DiagnosisStep step, int percentage, DiagnosisStatus status, String message) {
        log.debug("[DiagnosisService.sendProgress] ENTER - diagnosisId: {}, step: {}, percentage: {}%, status: {}",
            diagnosisId, step, percentage, status);
        log.debug("[DiagnosisService.sendProgress] Message: {}", message);

        try {
            DiagnosisProgressResponse progress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .currentStep(step)
                    .progressPercentage(percentage)
                    .status(status)
                    .currentMessage(message)
                    .build();
            log.debug("[DiagnosisService.sendProgress] Progress response created, sending via emitter manager");
            emitterManager.sendProgress(diagnosisId, progress);
            log.debug("[DiagnosisService.sendProgress] EXIT - progress sent successfully");
        } catch (Exception e) {
            log.error("[DiagnosisService.sendProgress] EXCEPTION - diagnosisId: {}, error: {}", diagnosisId, e.getMessage(), e);
        }
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
        log.info("[DiagnosisService.createNewDiagnosis] ENTER - memberId: {}", memberId);
        long startTime = System.currentTimeMillis();

        try {
            // 비관적 락으로 진행 중인 진단 확인 (TOCTOU 경쟁 조건 방지)
            List<DiagnosisStatus> inProgressStatuses = List.of(
                    DiagnosisStatus.IN_PROGRESS,
                    DiagnosisStatus.PENDING,
                    DiagnosisStatus.AWAITING_USER_INPUT
            );
            log.debug("[DiagnosisService.createNewDiagnosis] Checking for existing in-progress diagnosis with pessimistic lock");
            long lockStartTime = System.currentTimeMillis();

            Optional<DiagnosisResult> existingDiagnosis = diagnosisResultRepository
                    .findInProgressDiagnosisWithLock(memberId, inProgressStatuses);

            long lockDuration = System.currentTimeMillis() - lockStartTime;
            log.debug("[DiagnosisService.createNewDiagnosis] Lock acquired and check completed in {}ms", lockDuration);

            if (existingDiagnosis.isPresent()) {
                DiagnosisResult existing = existingDiagnosis.get();
                long duration = System.currentTimeMillis() - startTime;
                log.warn("[DiagnosisService.createNewDiagnosis] Diagnosis already in progress - memberId: {}, existingDiagnosisId: {}, status: {}, duration: {}ms",
                        memberId, existing.getId(), existing.getStatus(), duration);
                throw new DiagnosisAlreadyInProgressException(memberId);
            }

            // 진단 생성 및 즉시 IN_PROGRESS 상태로 전환
            log.debug("[DiagnosisService.createNewDiagnosis] Creating new diagnosis entity with PENDING status");
            DiagnosisResult diagnosis = DiagnosisResult.createPending(memberId);

            log.debug("[DiagnosisService.createNewDiagnosis] Saving diagnosis entity to DB");
            long saveStartTime = System.currentTimeMillis();
            DiagnosisResult saved = diagnosisResultRepository.save(diagnosis);
            long saveDuration = System.currentTimeMillis() - saveStartTime;
            log.debug("[DiagnosisService.createNewDiagnosis] Diagnosis saved in {}ms - diagnosisId: {}", saveDuration, saved.getId());

            // 즉시 IN_PROGRESS로 전환하여 경쟁 조건 최소화
            log.debug("[DiagnosisService.createNewDiagnosis] Transitioning diagnosis to IN_PROGRESS - diagnosisId: {}", saved.getId());
            saved.startDiagnosis();
            long transitionStartTime = System.currentTimeMillis();
            diagnosisResultRepository.save(saved);
            long transitionDuration = System.currentTimeMillis() - transitionStartTime;
            log.debug("[DiagnosisService.createNewDiagnosis] Status transition saved in {}ms", transitionDuration);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisService.createNewDiagnosis] EXIT - diagnosisId: {}, status: {}, totalDuration: {}ms",
                    saved.getId(), saved.getStatus(), totalDuration);

            return saved.getId();

        } catch (DiagnosisAlreadyInProgressException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[DiagnosisService.createNewDiagnosis] EXCEPTION (AlreadyInProgress) - memberId: {}, duration: {}ms",
                memberId, duration);
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisService.createNewDiagnosis] EXCEPTION - memberId: {}, duration: {}ms, error: {}",
                memberId, duration, e.getMessage(), e);
            throw e;
        }
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
        log.info("[DiagnosisService.findDiagnosisResult] ENTER - diagnosisId: {}, memberId: {}", diagnosisId, memberId);
        long startTime = System.currentTimeMillis();

        try {
            log.debug("[DiagnosisService.findDiagnosisResult] Fetching diagnosis from DB - diagnosisId: {}", diagnosisId);
            DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                    .orElseThrow(() -> {
                        log.error("[DiagnosisService.findDiagnosisResult] Diagnosis not found - diagnosisId: {}", diagnosisId);
                        return new DiagnosisNotFoundException(diagnosisId);
                    });
            log.debug("[DiagnosisService.findDiagnosisResult] Diagnosis fetched - status: {}, isDeleted: {}, isCompleted: {}",
                diagnosisResult.getStatus(), diagnosisResult.isDeleted(), diagnosisResult.isCompleted());

            // 소유권 확인
            log.debug("[DiagnosisService.findDiagnosisResult] Verifying ownership - diagnosisId: {}, memberId: {}", diagnosisId, memberId);
            verifyDiagnosisOwnership(diagnosisResult, memberId);
            log.debug("[DiagnosisService.findDiagnosisResult] Ownership verified successfully");

            // 삭제된 결과인 경우 예외 발생
            if (diagnosisResult.isDeleted()) {
                log.error("[DiagnosisService.findDiagnosisResult] Diagnosis result is deleted - diagnosisId: {}", diagnosisId);
                throw new DiagnosisNotFoundException("삭제된 진단 결과입니다. diagnosisId: " + diagnosisId);
            }

            // 완료되지 않은 진단인 경우
            if (!diagnosisResult.isCompleted()) {
                log.error("[DiagnosisService.findDiagnosisResult] Diagnosis not completed - diagnosisId: {}, status: {}",
                    diagnosisId, diagnosisResult.getStatus());
                throw new IllegalStateException(
                        String.format("완료되지 않은 진단입니다. diagnosisId: %d, status: %s",
                                diagnosisId, diagnosisResult.getStatus())
                );
            }

            // DiagnosisResultData를 DiagnosisResultResponse로 변환
            log.debug("[DiagnosisService.findDiagnosisResult] Converting result data to response DTO");
            com.shingu.roadmap.diagnosis.domain.DiagnosisResultData resultData = diagnosisResult.getResultData();

            DiagnosisResultResponse response = DiagnosisResultResponse.builder()
                    .diagnosisId(diagnosisId)
                    .summary(resultData.getSummary())
                    .ncsAnalyses(resultData.getNcsAnalyses())
                    .confidenceScore(resultData.getConfidenceScore())
                    .radarChartData(resultData.getRadarChartData())
                    .build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisService.findDiagnosisResult] EXIT - diagnosisId: {}, duration: {}ms, confidenceScore: {}, ncsAnalysesCount: {}",
                diagnosisId, duration, response.confidenceScore(),
                response.ncsAnalyses() != null ? response.ncsAnalyses().size() : 0);
            return response;

        } catch (DiagnosisNotFoundException | IllegalStateException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[DiagnosisService.findDiagnosisResult] EXCEPTION (Expected) - diagnosisId: {}, duration: {}ms, error: {}",
                diagnosisId, duration, e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisService.findDiagnosisResult] EXCEPTION - diagnosisId: {}, memberId: {}, duration: {}ms, error: {}",
                diagnosisId, memberId, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 진단 상태를 업데이트합니다.
     *
     * @param diagnosisId 진단 ID
     * @param status 진단 상태
     */
    @Transactional
    public void updateDiagnosisStatus(Long diagnosisId, DiagnosisStatus status) {
        log.info("[DiagnosisService.updateDiagnosisStatus] ENTER - diagnosisId: {}, targetStatus: {}", diagnosisId, status);
        long startTime = System.currentTimeMillis();

        try {
            log.debug("[DiagnosisService.updateDiagnosisStatus] Fetching diagnosis from DB - diagnosisId: {}", diagnosisId);
            DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                    .orElseThrow(() -> {
                        log.error("[DiagnosisService.updateDiagnosisStatus] Diagnosis not found - diagnosisId: {}", diagnosisId);
                        return new IllegalArgumentException("진단 정보를 찾을 수 없습니다. diagnosisId: " + diagnosisId);
                    });

            DiagnosisStatus currentStatus = diagnosisResult.getStatus();
            log.info("[DiagnosisService.updateDiagnosisStatus] Current status: {} -> Target status: {}", currentStatus, status);

            // 상태 전이 메서드 사용 (도메인 로직 활용)
            switch (status) {
                case IN_PROGRESS -> {
                    if (diagnosisResult.getStatus() == DiagnosisStatus.PENDING) {
                        log.debug("[DiagnosisService.updateDiagnosisStatus] Transitioning PENDING -> IN_PROGRESS");
                        diagnosisResult.startDiagnosis();
                    } else if (diagnosisResult.getStatus() == DiagnosisStatus.AWAITING_USER_INPUT) {
                        log.debug("[DiagnosisService.updateDiagnosisStatus] Transitioning AWAITING_USER_INPUT -> IN_PROGRESS (resume)");
                        diagnosisResult.resumeDiagnosis();
                    } else {
                        log.warn("[DiagnosisService.updateDiagnosisStatus] Unexpected transition to IN_PROGRESS from status: {}", currentStatus);
                    }
                }
                case AWAITING_USER_INPUT -> {
                    log.debug("[DiagnosisService.updateDiagnosisStatus] Transitioning to AWAITING_USER_INPUT");
                    diagnosisResult.awaitUserInput();
                }
                case FAILED -> {
                    log.debug("[DiagnosisService.updateDiagnosisStatus] Transitioning to FAILED (manual)");
                    diagnosisResult.failDiagnosis("Manual status update");
                }
                default -> log.warn("[DiagnosisService.updateDiagnosisStatus] Unexpected status transition to {} for diagnosisId: {}", status, diagnosisId);
            }

            log.debug("[DiagnosisService.updateDiagnosisStatus] Saving status change to DB");
            long saveStartTime = System.currentTimeMillis();
            diagnosisResultRepository.save(diagnosisResult);
            long saveDuration = System.currentTimeMillis() - saveStartTime;

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[DiagnosisService.updateDiagnosisStatus] EXIT - diagnosisId: {}, finalStatus: {}, saveDuration: {}ms, totalDuration: {}ms",
                diagnosisId, diagnosisResult.getStatus(), saveDuration, totalDuration);

        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisService.updateDiagnosisStatus] EXCEPTION (IllegalArgument) - diagnosisId: {}, duration: {}ms, error: {}",
                diagnosisId, duration, e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisService.updateDiagnosisStatus] EXCEPTION - diagnosisId: {}, duration: {}ms, error: {}",
                diagnosisId, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 사용자 선택을 반영하여 진단을 비동기로 계속 진행합니다. (SSE 지원)
     *
     * @param diagnosisId 진단 ID
     * @param selectedNcsCode 사용자가 선택한 NCS 코드
     */
    @Async("diagnosisTaskExecutor")
    public void continueWithUserSelectionAsync(Long diagnosisId, String selectedNcsCode) {
        log.info("[DiagnosisService.continueWithUserSelectionAsync] ENTER - diagnosisId: {}, selectedNcsCode: {}",
                diagnosisId, selectedNcsCode);
        long totalStartTime = System.currentTimeMillis();

        try {
            // 진단 ID로 회원 ID 조회 및 상태 업데이트 (Transactional)
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Resuming diagnosis after user input");
            long resumeStartTime = System.currentTimeMillis();
            Long memberId = diagnosisStateService.resumeDiagnosisAfterUserInput(diagnosisId);
            long resumeDuration = System.currentTimeMillis() - resumeStartTime;
            log.info("[DiagnosisService.continueWithUserSelectionAsync] Diagnosis resumed in {}ms - memberId: {}", resumeDuration, memberId);

            // 초기 상태 전송
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Sending initial continuation progress");
            sendProgress(diagnosisId, DiagnosisStep.JOB_MATCHING, 33, DiagnosisStatus.IN_PROGRESS,
                    "사용자 선택을 반영하여 진단을 계속합니다...");

            // 1. 사용자 프로필 및 모든 연관 데이터 조회 (Transactional 메서드 호출 - Fully Detached)
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Loading member data - memberId: {}", memberId);
            long loadStartTime = System.currentTimeMillis();
            MemberWithProfile memberData = diagnosisStateService.loadDiagnosisDataDetached(memberId);
            Member member = memberData.member();
            Profile profile = memberData.profile();
            long loadDuration = System.currentTimeMillis() - loadStartTime;
            log.info("[DiagnosisService.continueWithUserSelectionAsync] Member data loaded in {}ms - skillCount: {}, projectCount: {}",
                loadDuration,
                profile.getProfileSkills() != null ? profile.getProfileSkills().size() : 0,
                profile.getResume() != null && profile.getResume().getProjects() != null ?
                    profile.getResume().getProjects().size() : 0);

            // 2. 진단 컨텍스트 초기화 (1단계는 건너뛰고 2단계부터 시작)
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Initializing continuation context with user-selected NCS code: {}",
                selectedNcsCode);
            DiagnosisContext context = DiagnosisContext.builder()
                    .memberId(memberId)
                    .diagnosisId(diagnosisId)
                    .profile(profile)
                    .userSelectedNcsCode(selectedNcsCode)
                    .success(true)
                    .progressCallback(progress -> emitterManager.sendProgress(diagnosisId, progress))
                    .build();
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Context initialized successfully");

            // 3. 2단계부터 파이프라인 실행
            List<DiagnosisProcessor> processors = List.of(
                    competencyAnalysisProcessor,
                    reportGenerationProcessor
            );
            log.info("[DiagnosisService.continueWithUserSelectionAsync] Starting continuation pipeline with {} processors (skipping NCS recommendation)",
                processors.size());

            // 4. 파이프라인 실행 (진행 상황 전송 포함)
            long pipelineStartTime = System.currentTimeMillis();
            context = executePipelineWithProgress(processors, context);
            long pipelineDuration = System.currentTimeMillis() - pipelineStartTime;
            log.info("[DiagnosisService.continueWithUserSelectionAsync] Continuation pipeline execution completed in {}ms - success: {}",
                pipelineDuration, context.isSuccess());

            // 5. 결과 확인 및 완료 처리
            if (!context.isSuccess()) {
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.error("[DiagnosisService.continueWithUserSelectionAsync] Diagnosis continuation FAILED - diagnosisId: {}, totalDuration: {}ms, error: {}",
                        diagnosisId, totalDuration, context.getErrorMessage());

                // 진단 실패 상태 DB에 저장
                log.debug("[DiagnosisService.continueWithUserSelectionAsync] Saving failure state to DB");
                diagnosisStateService.failDiagnosisWithError(diagnosisId, context.getErrorMessage());

                DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                        .diagnosisId(diagnosisId)
                        .status(DiagnosisStatus.FAILED)
                        .currentStep(DiagnosisStep.FINAL_REPORT)
                        .progressPercentage(100)
                        .currentMessage("진단 중 오류가 발생했습니다: " + context.getErrorMessage())
                        .build();
                log.debug("[DiagnosisService.continueWithUserSelectionAsync] Sending error completion via SSE");
                emitterManager.completeWithError(diagnosisId, errorProgress);
                log.info("[DiagnosisService.continueWithUserSelectionAsync] EXIT (FAILED) - diagnosisId: {}, totalDuration: {}ms",
                    diagnosisId, totalDuration);
                return;
            }

            // 6. 진단 결과 DB에 저장 (Transactional 메서드 호출)
            DiagnosisResultResponse diagnosisResultResponse = context.getDiagnosisResultResponse();
            if (diagnosisResultResponse != null) {
                log.debug("[DiagnosisService.continueWithUserSelectionAsync] Saving continuation diagnosis result to DB");
                long saveStartTime = System.currentTimeMillis();
                diagnosisStateService.saveDiagnosisResultData(diagnosisId, diagnosisResultResponse);
                long saveDuration = System.currentTimeMillis() - saveStartTime;
                log.info("[DiagnosisService.continueWithUserSelectionAsync] Result saved to DB in {}ms", saveDuration);
            } else {
                log.warn("[DiagnosisService.continueWithUserSelectionAsync] No diagnosis result response to save - diagnosisId: {}", diagnosisId);
            }

            // 7. 완료 상태 전송
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.info("[DiagnosisService.continueWithUserSelectionAsync] Diagnosis continuation COMPLETED successfully - diagnosisId: {}, totalDuration: {}ms",
                diagnosisId, totalDuration);
            DiagnosisProgressResponse finalProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.COMPLETED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단이 완료되었습니다.")
                    .build();
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Sending completion via SSE");
            emitterManager.complete(diagnosisId, finalProgress);
            log.info("[DiagnosisService.continueWithUserSelectionAsync] EXIT (SUCCESS) - diagnosisId: {}, totalDuration: {}ms",
                diagnosisId, totalDuration);

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.error("[DiagnosisService.continueWithUserSelectionAsync] EXCEPTION - diagnosisId: {}, selectedNcsCode: {}, totalDuration: {}ms, error: {}",
                diagnosisId, selectedNcsCode, totalDuration, e.getMessage(), e);

            // 진단 실패 상태 DB에 저장
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Saving unexpected error state to DB");
            diagnosisStateService.failDiagnosisWithError(diagnosisId, "진단 중 예기치 않은 오류가 발생했습니다: " + e.getMessage());

            DiagnosisProgressResponse errorProgress = DiagnosisProgressResponse.builder()
                    .diagnosisId(diagnosisId)
                    .status(DiagnosisStatus.FAILED)
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(100)
                    .currentMessage("진단 중 예기치 않은 오류가 발생했습니다.")
                    .build();
            log.debug("[DiagnosisService.continueWithUserSelectionAsync] Sending exception completion via SSE");
            emitterManager.completeWithError(diagnosisId, errorProgress);
            log.info("[DiagnosisService.continueWithUserSelectionAsync] EXIT (EXCEPTION) - diagnosisId: {}, totalDuration: {}ms",
                diagnosisId, totalDuration);
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
        log.debug("[DiagnosisService.verifyDiagnosisOwnershipById] ENTER - diagnosisId: {}, memberId: {}", diagnosisId, memberId);
        long startTime = System.currentTimeMillis();

        try {
            log.debug("[DiagnosisService.verifyDiagnosisOwnershipById] Fetching diagnosis from DB - diagnosisId: {}", diagnosisId);
            DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                    .orElseThrow(() -> {
                        log.error("[DiagnosisService.verifyDiagnosisOwnershipById] Diagnosis not found - diagnosisId: {}", diagnosisId);
                        return new DiagnosisNotFoundException(diagnosisId);
                    });

            log.debug("[DiagnosisService.verifyDiagnosisOwnershipById] Diagnosis fetched - ownerId: {}", diagnosisResult.getMemberId());
            verifyDiagnosisOwnership(diagnosisResult, memberId);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("[DiagnosisService.verifyDiagnosisOwnershipById] EXIT - ownership verified successfully, duration: {}ms", duration);

        } catch (DiagnosisNotFoundException | DiagnosisAccessDeniedException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("[DiagnosisService.verifyDiagnosisOwnershipById] EXCEPTION (Expected) - diagnosisId: {}, memberId: {}, duration: {}ms, error: {}",
                diagnosisId, memberId, duration, e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[DiagnosisService.verifyDiagnosisOwnershipById] EXCEPTION - diagnosisId: {}, memberId: {}, duration: {}ms, error: {}",
                diagnosisId, memberId, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 진단 결과의 소유권을 확인합니다. (Private 헬퍼 메서드)
     *
     * @param diagnosisResult 진단 결과 엔티티
     * @param memberId 조회하는 회원 ID
     * @throws DiagnosisAccessDeniedException 소유권이 없는 경우
     */
    private void verifyDiagnosisOwnership(DiagnosisResult diagnosisResult, Long memberId) {
        log.debug("[DiagnosisService.verifyDiagnosisOwnership] ENTER - diagnosisId: {}, requestedBy: {}, owner: {}",
            diagnosisResult.getId(), memberId, diagnosisResult.getMemberId());

        if (!diagnosisResult.getMemberId().equals(memberId)) {
            log.warn("[DiagnosisService.verifyDiagnosisOwnership] Ownership verification FAILED - diagnosisId: {}, requestedBy: {}, actualOwner: {}",
                    diagnosisResult.getId(), memberId, diagnosisResult.getMemberId());
            throw new DiagnosisAccessDeniedException(diagnosisResult.getId(), memberId);
        }

        log.debug("[DiagnosisService.verifyDiagnosisOwnership] EXIT - ownership verified successfully");
    }
}
