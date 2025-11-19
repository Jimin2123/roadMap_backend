package com.shingu.roadmap.apis.openai.service;

import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import com.shingu.roadmap.apis.openai.service.workflow.*;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI Service - 워크플로우 기반 Facade 패턴 적용
 * 각 비즈니스 워크플로우를 전문 서비스에 위임합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {

    private final TrainingRecommendationWorkflow trainingRecommendationWorkflow;
    private final CareerNetSearchCodeWorkflow careerNetSearchCodeWorkflow;
    private final NcsCodeRecommendationWorkflow ncsCodeRecommendationWorkflow;
    private final KeywordGenerationWorkflow keywordGenerationWorkflow;
    private final NcsCompetencyAnalysisWorkflow ncsCompetencyAnalysisWorkflow;

    /**
     * 사용자 정보를 바탕으로 부족한 역량을 보완할 훈련과정을 추천합니다.
     */
    public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {
        return trainingRecommendationWorkflow.recommendTrainingCourse(request);
    }

    /**
     * 사용자의 상세 정보를 바탕으로 커리어넷 API 검색에 적합한 분류 코드를 추천합니다.
     */
    public Mono<Map<String, String>> recommendSearchCodes(Profile profile) {
        return careerNetSearchCodeWorkflow.recommendSearchCodes(profile);
    }

    /**
     * 희망 직무 기반 NCS 코드 추천
     */
    public Mono<Set<String>> recommendDesiredJobCodeUsingAssistant(String desiredJob) {
        return ncsCodeRecommendationWorkflow.recommendDesiredJobCodeUsingAssistant(desiredJob);
    }

    /**
     * Profile 객체를 받아 사용자의 실제 역량에 기반한 NCS 코드를 추천합니다.
     */
    public Mono<Set<String>> recommendNcsCodeUsingAssistant(Profile profile) {
        return ncsCodeRecommendationWorkflow.recommendNcsCodeUsingAssistant(profile);
    }

    /**
     * 사용자 프로필 기반 커리어 키워드 생성
     */
    public Mono<Set<String>> generateKeyword(Profile profile) {
        return keywordGenerationWorkflow.generateKeyword(profile);
    }

    /**
     * AI 기반 KSA 역량 분석
     */
    public Mono<Map<String, NcsCompetencyAnalysisWorkflow.KsaEvaluationResult>> analyzeKsaCompetency(
            String ncsCode,
            List<String> ksaItems,
            Profile profile
    ) {
        return ncsCompetencyAnalysisWorkflow.analyzeKsaCompetency(ncsCode, ksaItems, profile);
    }

    /**
     * AI 기반 NCS 적합도 신뢰도 평가
     */
    public Mono<NcsCompetencyAnalysisWorkflow.NcsConfidenceEvaluation> evaluateNcsMatchConfidence(
            String ncsCode,
            String ncsName,
            List<String> compUnitNames,
            Profile profile
    ) {
        return ncsCompetencyAnalysisWorkflow.evaluateNcsMatchConfidence(ncsCode, ncsName, compUnitNames, profile);
    }
}
