package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.apis.openai.service.workflow.JobRecommendationWorkflow;
import com.shingu.roadmap.diagnosis.dto.response.JobRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 채용공고 추천 프로세서
 * 사용자 프로필과 추천된 NCS 코드를 기반으로 적합한 채용공고를 추천합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobRecommendationProcessor implements DiagnosisProcessor {

    private final JobRecommendationWorkflow jobRecommendationWorkflow;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[JobRecommendationProcessor] ENTER - diagnosisId: {}, memberId: {}",
                context.getDiagnosisId(), context.getMemberId());
        long startTime = System.currentTimeMillis();

        try {
            // 1. NCS 분석 결과 확인
            NcsAnalysisResponse ncsAnalysis = context.getNcsAnalysisResponse();
            if (ncsAnalysis == null || ncsAnalysis.candidates() == null ||
                    ncsAnalysis.candidates().isEmpty()) {
                log.warn("[JobRecommendationProcessor] No NCS codes available for job recommendation");
                context.setJobRecommendations(Collections.emptyList());
                long duration = System.currentTimeMillis() - startTime;
                log.info("[JobRecommendationProcessor] EXIT (No NCS codes) - duration: {}ms", duration);
                return context;
            }

            // 2. 사용자가 선택한 NCS 코드 또는 최우선 추천 코드 선택
            String targetNcsCode = context.getUserSelectedNcsCode() != null ?
                    context.getUserSelectedNcsCode() :
                    ncsAnalysis.candidates().get(0).ncsCode();

            log.info("[JobRecommendationProcessor] Target NCS code for job recommendation: {}", targetNcsCode);

            // 3. 채용공고 추천 워크플로우 실행
            List<JobRecommendationResponse> jobRecommendations = jobRecommendationWorkflow
                    .recommendJobs(context.getProfile(), targetNcsCode)
                    .block(); // 동기 처리 (파이프라인 순차 실행)

            if (jobRecommendations == null || jobRecommendations.isEmpty()) {
                log.warn("[JobRecommendationProcessor] No job recommendations generated");
                context.setJobRecommendations(Collections.emptyList());
            } else {
                log.info("[JobRecommendationProcessor] Successfully recommended {} jobs", jobRecommendations.size());
                context.setJobRecommendations(jobRecommendations);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[JobRecommendationProcessor] EXIT (SUCCESS) - duration: {}ms, jobCount: {}",
                    duration, jobRecommendations != null ? jobRecommendations.size() : 0);

            return context;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[JobRecommendationProcessor] EXCEPTION - diagnosisId: {}, duration: {}ms, error: {}",
                    context.getDiagnosisId(), duration, e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("채용공고 추천 중 오류가 발생했습니다: " + e.getMessage());
            log.info("[JobRecommendationProcessor] EXIT (FAILED) - duration: {}ms", duration);
            return context;
        }
    }

    @Override
    public String getName() {
        return "JobRecommendationProcessor";
    }
}
