package com.shingu.roadmap.diagnosis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.diagnosis.domain.DiagnosisResult;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.repository.DiagnosisResultRepository;
import com.shingu.roadmap.diagnosis.service.pipeline.*;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final MemberRepository memberRepository;
    private final DiagnosisResultRepository diagnosisResultRepository;
    private final NcsRecommendationProcessor ncsRecommendationProcessor;
    private final CompetencyAnalysisProcessor competencyAnalysisProcessor;
    private final ReportGenerationProcessor reportGenerationProcessor;
    private final DiagnosisEmitterManager emitterManager;
    private final ObjectMapper objectMapper;

    // 프로세서별 진행률 및 메시지 매핑
    private static final Map<String, ProcessorProgress> PROCESSOR_PROGRESS_MAP = Map.of(
            "NcsRecommendationProcessor", new ProcessorProgress(DiagnosisStep.NCS_CODE_SUGGESTION, 33, "NCS 직무 코드를 추천하고 있습니다..."),
            "CompetencyAnalysisProcessor", new ProcessorProgress(DiagnosisStep.JOB_MATCHING, 66, "역량을 분석하고 있습니다..."),
            "ReportGenerationProcessor", new ProcessorProgress(DiagnosisStep.FINAL_REPORT, 90, "최종 보고서를 생성하고 있습니다...")
    );

    private record ProcessorProgress(DiagnosisStep step, int percentage, String message) {}

    /**
     * 전체 진단 프로세스 실행
     *
     * @param memberId 진단 대상 회원 ID
     * @return 최종 진단 결과
     */
    @Transactional(readOnly = true)
    public DiagnosisResultResponse executeDiagnosis(Long memberId) {
        log.info("Starting diagnosis for memberId: {}", memberId);

        // 1. 사용자 프로필 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for memberId: " + memberId));

        Profile profile = member.getProfile();
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found for memberId: " + memberId);
        }

        // 2. 진단 컨텍스트 초기화
        DiagnosisContext context = DiagnosisContext.builder()
                .memberId(memberId)
                .profile(profile)
                .success(true)
                .build();

        // 3. 파이프라인 프로세서 목록
        List<DiagnosisProcessor> processors = List.of(
                ncsRecommendationProcessor,
                competencyAnalysisProcessor,
                reportGenerationProcessor
        );

        // 4. 파이프라인 실행
        context = executePipeline(processors, context);

        // 5. 결과 반환
        if (!context.isSuccess()) {
            throw new RuntimeException("Diagnosis failed: " + context.getErrorMessage());
        }

        log.info("Diagnosis completed successfully for memberId: {}", memberId);
        return context.getDiagnosisResultResponse();
    }

    /**
     * 사용자 선택을 반영하여 진단 계속 진행
     *
     * @param memberId 진단 대상 회원 ID
     * @param selectedNcsCode 사용자가 선택한 NCS 코드
     * @return 최종 진단 결과
     */
    @Transactional(readOnly = true)
    public DiagnosisResultResponse continueWithUserSelection(Long memberId, String selectedNcsCode) {
        log.info("Continuing diagnosis for memberId: {} with user selected NCS code: {}", memberId, selectedNcsCode);

        // 1. 사용자 프로필 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for memberId: " + memberId));

        Profile profile = member.getProfile();
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found for memberId: " + memberId);
        }

        // 2. 진단 컨텍스트 초기화 (1단계는 건너뛰고 2단계부터 시작)
        DiagnosisContext context = DiagnosisContext.builder()
                .memberId(memberId)
                .profile(profile)
                .userSelectedNcsCode(selectedNcsCode)
                .success(true)
                .build();

        // 3. 2단계부터 파이프라인 실행
        List<DiagnosisProcessor> processors = List.of(
                competencyAnalysisProcessor,
                reportGenerationProcessor
        );

        // 4. 파이프라인 실행
        context = executePipeline(processors, context);

        // 5. 결과 반환
        if (!context.isSuccess()) {
            throw new RuntimeException("Diagnosis continuation failed: " + context.getErrorMessage());
        }

        log.info("Diagnosis completed successfully for memberId: {}", memberId);
        return context.getDiagnosisResultResponse();
    }

    /**
     * 파이프라인 프로세서 순차 실행
     *
     * @param processors 프로세서 목록
     * @param context 진단 컨텍스트
     * @return 처리된 진단 컨텍스트
     */
    private DiagnosisContext executePipeline(List<DiagnosisProcessor> processors, DiagnosisContext context) {
        for (DiagnosisProcessor processor : processors) {
            log.info("Executing processor: {}", processor.getName());

            try {
                context = processor.process(context);

                if (!context.isSuccess()) {
                    log.error("Processor {} failed: {}", processor.getName(), context.getErrorMessage());
                    break;
                }

                log.info("Processor {} completed successfully", processor.getName());

            } catch (Exception e) {
                log.error("Processor {} threw exception: {}", processor.getName(), e.getMessage(), e);
                context.setSuccess(false);
                context.setErrorMessage("프로세서 실행 중 오류 발생: " + processor.getName());
                break;
            }
        }

        return context;
    }

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

            // 1. 사용자 프로필 조회
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Member not found for memberId: " + memberId));

            Profile profile = member.getProfile();
            if (profile == null) {
                throw new IllegalArgumentException("Profile not found for memberId: " + memberId);
            }

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

            // 6. 진단 결과 DB에 저장
            DiagnosisResultResponse diagnosisResult = context.getDiagnosisResultResponse();
            if (diagnosisResult != null) {
                try {
                    saveDiagnosisResult(diagnosisResult);
                    log.info("Diagnosis result saved to database for diagnosisId: {}", diagnosisId);
                } catch (Exception e) {
                    log.error("Failed to save diagnosis result to database for diagnosisId: {}", diagnosisId, e);
                    // 저장 실패해도 진단 자체는 완료된 것으로 처리 (SSE는 정상 완료)
                }
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
     * 진단 결과를 DB에 저장합니다.
     *
     * @param diagnosisResultResponse 진단 결과 응답 DTO
     * @return 저장된 진단 결과 엔티티
     */
    @Transactional
    public DiagnosisResult saveDiagnosisResult(DiagnosisResultResponse diagnosisResultResponse) {
        log.info("Saving diagnosis result for diagnosisId: {}", diagnosisResultResponse.diagnosisId());

        try {
            // DiagnosisResultResponse를 JSON으로 직렬화
            String resultJson = objectMapper.writeValueAsString(diagnosisResultResponse);

            // 기존 진단 결과가 있는지 확인 (diagnosisId가 id와 같다고 가정)
            DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisResultResponse.diagnosisId())
                    .orElse(DiagnosisResult.builder()
                            .memberId(diagnosisResultResponse.diagnosisId()) // TODO: diagnosisId를 memberId로 사용하는 것은 임시 방편
                            .build());

            // 진단 결과 업데이트
            diagnosisResult.updateResult(
                    DiagnosisStatus.COMPLETED,
                    resultJson,
                    diagnosisResultResponse.confidenceScore(),
                    diagnosisResultResponse.summary()
            );

            // 저장
            DiagnosisResult saved = diagnosisResultRepository.save(diagnosisResult);
            log.info("Diagnosis result saved successfully with id: {}", saved.getId());

            return saved;

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DiagnosisResultResponse to JSON", e);
            throw new RuntimeException("진단 결과 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 진단 ID로 진단 결과를 조회합니다.
     *
     * @param diagnosisId 진단 ID
     * @return 진단 결과 응답 DTO
     */
    @Transactional(readOnly = true)
    public DiagnosisResultResponse findDiagnosisResult(Long diagnosisId) {
        log.info("Finding diagnosis result for diagnosisId: {}", diagnosisId);

        DiagnosisResult diagnosisResult = diagnosisResultRepository.findById(diagnosisId)
                .orElseThrow(() -> new IllegalArgumentException("진단 결과를 찾을 수 없습니다. diagnosisId: " + diagnosisId));

        // 삭제된 결과인 경우 예외 발생
        if (diagnosisResult.isDeleted()) {
            throw new IllegalArgumentException("삭제된 진단 결과입니다. diagnosisId: " + diagnosisId);
        }

        try {
            // JSON을 DiagnosisResultResponse로 역직렬화
            DiagnosisResultResponse response = objectMapper.readValue(
                    diagnosisResult.getResultJson(),
                    DiagnosisResultResponse.class
            );

            log.info("Diagnosis result found successfully for diagnosisId: {}", diagnosisId);
            return response;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to DiagnosisResultResponse", e);
            throw new RuntimeException("진단 결과 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회원 ID로 가장 최근 진단 결과를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 가장 최근 진단 결과 응답 DTO
     */
    @Transactional(readOnly = true)
    public DiagnosisResultResponse findLatestDiagnosisResultByMemberId(Long memberId) {
        log.info("Finding latest diagnosis result for memberId: {}", memberId);

        DiagnosisResult diagnosisResult = diagnosisResultRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new IllegalArgumentException("진단 결과를 찾을 수 없습니다. memberId: " + memberId));

        try {
            // JSON을 DiagnosisResultResponse로 역직렬화
            DiagnosisResultResponse response = objectMapper.readValue(
                    diagnosisResult.getResultJson(),
                    DiagnosisResultResponse.class
            );

            log.info("Latest diagnosis result found successfully for memberId: {}", memberId);
            return response;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to DiagnosisResultResponse", e);
            throw new RuntimeException("진단 결과 조회 중 오류가 발생했습니다.", e);
        }
    }
}
