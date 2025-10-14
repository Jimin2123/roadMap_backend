package com.shingu.roadmap.diagnosis.service;

import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.service.pipeline.*;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final NcsRecommendationProcessor ncsRecommendationProcessor;
    private final CompetencyAnalysisProcessor competencyAnalysisProcessor;
    private final ReportGenerationProcessor reportGenerationProcessor;

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
}
