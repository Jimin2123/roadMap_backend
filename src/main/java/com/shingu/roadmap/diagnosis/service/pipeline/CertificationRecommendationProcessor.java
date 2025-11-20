package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.apis.openai.service.workflow.CertificationRecommendationWorkflow;
import com.shingu.roadmap.diagnosis.dto.response.CertificationRecommendationResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 자격증 추천 프로세서
 * 사용자의 역량 gap 분석 결과를 기반으로 필요한 자격증을 추천합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CertificationRecommendationProcessor implements DiagnosisProcessor {

    private final CertificationRecommendationWorkflow certificationRecommendationWorkflow;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[CertificationRecommendationProcessor] ENTER - diagnosisId: {}, memberId: {}",
                context.getDiagnosisId(), context.getMemberId());
        long startTime = System.currentTimeMillis();

        try {
            // 1. NCS 분석 결과 및 KSA 분석 결과 확인
            NcsAnalysisResponse ncsAnalysis = context.getNcsAnalysisResponse();
            List<KsaAnalysisResponse> ksaAnalyses = context.getKsaAnalysisResponses();

            if (ncsAnalysis == null || ncsAnalysis.candidates() == null ||
                    ncsAnalysis.candidates().isEmpty()) {
                log.warn("[CertificationRecommendationProcessor] No NCS codes available for certification recommendation");
                context.setCertificationRecommendations(Collections.emptyList());
                long duration = System.currentTimeMillis() - startTime;
                log.info("[CertificationRecommendationProcessor] EXIT (No NCS codes) - duration: {}ms", duration);
                return context;
            }

            if (ksaAnalyses == null || ksaAnalyses.isEmpty()) {
                log.warn("[CertificationRecommendationProcessor] No KSA analysis available for certification recommendation");
                context.setCertificationRecommendations(Collections.emptyList());
                long duration = System.currentTimeMillis() - startTime;
                log.info("[CertificationRecommendationProcessor] EXIT (No KSA analysis) - duration: {}ms", duration);
                return context;
            }

            // 2. 사용자가 선택한 NCS 코드 또는 최우선 추천 코드 선택
            String targetNcsCode = context.getUserSelectedNcsCode() != null ?
                    context.getUserSelectedNcsCode() :
                    ncsAnalysis.candidates().get(0).ncsCode();

            log.info("[CertificationRecommendationProcessor] Target NCS code for certification recommendation: {}",
                    targetNcsCode);

            // 3. 해당 NCS 코드에 대응하는 KSA 분석 결과 찾기
            KsaAnalysisResponse targetKsaAnalysis = ksaAnalyses.stream()
                    .filter(ksa -> targetNcsCode.equals(ksa.ncsCode()))
                    .findFirst()
                    .orElse(ksaAnalyses.get(0)); // fallback: 첫 번째 KSA 분석 사용

            log.info("[CertificationRecommendationProcessor] Using KSA analysis for NCS code: {}",
                    targetKsaAnalysis.ncsCode());

            // 4. 자격증 추천 워크플로우 실행
            List<CertificationRecommendationResponse> certificationRecommendations =
                    certificationRecommendationWorkflow
                            .recommendCertifications(context.getProfile(), targetNcsCode, targetKsaAnalysis)
                            .block(); // 동기 처리 (파이프라인 순차 실행)

            if (certificationRecommendations == null || certificationRecommendations.isEmpty()) {
                log.warn("[CertificationRecommendationProcessor] No certification recommendations generated");
                context.setCertificationRecommendations(Collections.emptyList());
            } else {
                log.info("[CertificationRecommendationProcessor] Successfully recommended {} certifications",
                        certificationRecommendations.size());
                context.setCertificationRecommendations(certificationRecommendations);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[CertificationRecommendationProcessor] EXIT (SUCCESS) - duration: {}ms, certCount: {}",
                    duration, certificationRecommendations != null ? certificationRecommendations.size() : 0);

            return context;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[CertificationRecommendationProcessor] EXCEPTION - diagnosisId: {}, duration: {}ms, error: {}",
                    context.getDiagnosisId(), duration, e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("자격증 추천 중 오류가 발생했습니다: " + e.getMessage());
            log.info("[CertificationRecommendationProcessor] EXIT (FAILED) - duration: {}ms", duration);
            return context;
        }
    }

    @Override
    public String getName() {
        return "CertificationRecommendationProcessor";
    }
}
