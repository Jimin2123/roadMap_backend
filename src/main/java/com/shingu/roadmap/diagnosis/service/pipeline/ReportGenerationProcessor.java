package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.apis.careernet.dto.request.JobInformationRequest;
import com.shingu.roadmap.apis.careernet.dto.response.JobDetailResponse;
import com.shingu.roadmap.apis.careernet.service.CareerNetService;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import com.shingu.roadmap.diagnosis.dto.common.*;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisResultResponse;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 3단계: 최종 진단 리포트 생성 프로세서
 *
 * 처리 흐름:
 * 3-1. 커리어넷 API로 직업 정보(전망, 임금 등) 조회
 * 3-2. 모든 분석 결과를 종합하여 최종 리포트 생성
 * 3-3. 레이더 차트 데이터 생성 (역량 시각화)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationProcessor implements DiagnosisProcessor {

    private final CareerNetService careerNetService;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[ReportGenerationProcessor] Starting report generation for memberId: {}", context.getMemberId());

        try {
            NcsAnalysisResponse ncsAnalysis = context.getNcsAnalysisResponse();
            List<KsaAnalysisResponse> ksaAnalyses = context.getKsaAnalysisResponses();
            String careerLevel = context.getCareerLevel();

            if (ncsAnalysis == null || ksaAnalyses == null || ksaAnalyses.isEmpty()) {
                throw new IllegalArgumentException("NCS and KSA analysis results are required for report generation");
            }

            reportProgress(context, 66, "커리어넷 직업 정보를 조회하고 있습니다.");

            // 3-1. 커리어넷 직업 정보 조회 및 보강
            NcsAnalysisResponse enrichedNcsAnalysis = enrichWithCareerNetInfo(ncsAnalysis, context);

            reportProgress(context, 85, "레이더 차트 데이터를 생성하고 있습니다.");

            // 3-2. 레이더 차트 데이터 생성
            RadarChartData radarChartData = generateRadarChartData(ksaAnalyses);

            reportProgress(context, 95, "종합 요약을 생성하고 있습니다.");

            // 3-3. 종합 요약 생성
            String summary = generateSummary(enrichedNcsAnalysis, ksaAnalyses, careerLevel);

            // 3-4. 최종 리포트 생성
            DiagnosisResultResponse diagnosisResult = DiagnosisResultResponse.builder()
                    .diagnosisId(null) // 실제 저장 시 할당
                    .summary(summary)
                    .ncsAnalyses(List.of(enrichedNcsAnalysis))
                    .confidenceScore(enrichedNcsAnalysis.overallConfidence())
                    .radarChartData(radarChartData)
                    .build();

            context.setDiagnosisResultResponse(diagnosisResult);
            context.setSuccess(true);

            reportProgress(context, 100, "진단 리포트가 완료되었습니다.");

            log.info("[ReportGenerationProcessor] Completed. Report generated successfully.");

            return context;

        } catch (Exception e) {
            log.error("[ReportGenerationProcessor] Failed to process: {}", e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("리포트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return context;
        }
    }

    /**
     * 커리어넷 직업 정보로 NCS 분석 결과 보강
     */
    private NcsAnalysisResponse enrichWithCareerNetInfo(NcsAnalysisResponse ncsAnalysis, DiagnosisContext context) {
        List<NcsRecommendationCandidate> enrichedCandidates = new ArrayList<>();
        List<NcsRecommendationCandidate> candidates = ncsAnalysis.candidates();
        int totalCandidates = candidates.size();
        int processedCount = 0;

        for (NcsRecommendationCandidate candidate : candidates) {
            try {
                // 커리어넷 직업 정보 조회
                JobDetailResponse jobDetail = fetchCareerNetJobInfo(candidate.ncsCode());

                if (jobDetail != null && jobDetail.getContent() != null) {
                    var jobInfo = jobDetail.getContent();

                    CareerNetJobInfo careerNetJobInfo = CareerNetJobInfo.builder()
                            .jobName(jobInfo.getJob())
                            .prospect(jobInfo.getProspect())
                            .salaryLevel(jobInfo.getSalery())
                            .build();

                    // 후보 정보에 커리어넷 정보 추가
                    enrichedCandidates.add(candidate.toBuilder()
                            .careerNetJobInfo(careerNetJobInfo)
                            .build());
                } else {
                    enrichedCandidates.add(candidate);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch CareerNet info for NCS code {}: {}", candidate.ncsCode(), e.getMessage());
                enrichedCandidates.add(candidate);
            }

            processedCount++;
            // 66% ~ 85% 범위에서 진행률 업데이트
            int progress = 66 + (19 * processedCount / totalCandidates);
            reportProgress(context, progress,
                    String.format("직업 정보 조회 중... (%d/%d)", processedCount, totalCandidates));
        }

        return ncsAnalysis.toBuilder()
                .candidates(enrichedCandidates)
                .build();
    }

    /**
     * 커리어넷 직업 정보 조회
     */
    private JobDetailResponse fetchCareerNetJobInfo(String ncsCode) {
        try {
            // NCS 코드를 커리어넷 직업 코드로 매핑 (임시로 동일 코드 사용)
            JobInformationRequest request = new JobInformationRequest();
            request.setSvcType("api");
            request.setSvcCode("JOB_VIEW");
            request.setContentType("json");
            request.setGubun("job_dic_list");
            request.setJobdicSeq(ncsCode);

            return careerNetService.getJobInformation(request);
        } catch (Exception e) {
            log.error("Failed to fetch job information from CareerNet: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 레이더 차트 데이터 생성
     */
    private RadarChartData generateRadarChartData(List<KsaAnalysisResponse> ksaAnalyses) {
        if (ksaAnalyses.isEmpty()) {
            return RadarChartData.builder()
                    .userProfile(null)
                    .targetNcsProfiles(List.of())
                    .competencyAxes(List.of())
                    .build();
        }

        // 첫 번째 KSA 분석 결과 기준
        KsaAnalysisResponse ksaAnalysis = ksaAnalyses.getFirst();

        // 1. 공통 역량 축 생성 (모든 KSA 항목명)
        List<String> competencyAxes = new ArrayList<>();
        competencyAxes.addAll(ksaAnalysis.knowledgeItems().stream()
                .map(KsaAnalysisResponse.KsaItem::itemName)
                .limit(5)
                .toList());
        competencyAxes.addAll(ksaAnalysis.skillItems().stream()
                .map(KsaAnalysisResponse.KsaItem::itemName)
                .limit(5)
                .toList());
        competencyAxes.addAll(ksaAnalysis.attitudeItems().stream()
                .map(KsaAnalysisResponse.KsaItem::itemName)
                .limit(3)
                .toList());

        // 2. 사용자 역량 프로필 생성
        Map<String, Double> userCompetencyScores = new HashMap<>();

        // Knowledge 점수
        ksaAnalysis.knowledgeItems().stream()
                .limit(5)
                .forEach(item -> userCompetencyScores.put(item.itemName(), item.userScore()));

        // Skill 점수
        ksaAnalysis.skillItems().stream()
                .limit(5)
                .forEach(item -> userCompetencyScores.put(item.itemName(), item.userScore()));

        // Attitude 점수
        ksaAnalysis.attitudeItems().stream()
                .limit(3)
                .forEach(item -> userCompetencyScores.put(item.itemName(), item.userScore()));

        CompetencyProfile userProfile = CompetencyProfile.builder()
                .profileName("현재 보유 역량")
                .competencyScores(userCompetencyScores)
                .build();

        // 3. 목표 NCS 역량 프로필 생성
        Map<String, Double> targetCompetencyScores = new HashMap<>();

        // Knowledge 목표 점수
        ksaAnalysis.knowledgeItems().stream()
                .limit(5)
                .forEach(item -> targetCompetencyScores.put(item.itemName(), item.targetScore()));

        // Skill 목표 점수
        ksaAnalysis.skillItems().stream()
                .limit(5)
                .forEach(item -> targetCompetencyScores.put(item.itemName(), item.targetScore()));

        // Attitude 목표 점수
        ksaAnalysis.attitudeItems().stream()
                .limit(3)
                .forEach(item -> targetCompetencyScores.put(item.itemName(), item.targetScore()));

        // 일치율 계산
        double matchRate = calculateMatchRate(userCompetencyScores, targetCompetencyScores);

        NcsCompetencyProfile targetProfile = NcsCompetencyProfile.builder()
                .ncsCode(ksaAnalysis.ncsCode())
                .ncsName("목표 직무 요구 역량")
                .competencyScores(targetCompetencyScores)
                .matchRate(matchRate)
                .build();

        return RadarChartData.builder()
                .userProfile(userProfile)
                .targetNcsProfiles(List.of(targetProfile))
                .competencyAxes(competencyAxes)
                .build();
    }

    /**
     * 사용자 역량과 목표 역량의 일치율 계산
     */
    private double calculateMatchRate(Map<String, Double> userScores, Map<String, Double> targetScores) {
        if (userScores.isEmpty() || targetScores.isEmpty()) {
            return 0.0;
        }

        double totalMatch = 0.0;
        int count = 0;

        for (Map.Entry<String, Double> entry : targetScores.entrySet()) {
            String competency = entry.getKey();
            Double targetScore = entry.getValue();
            Double userScore = userScores.get(competency);

            if (userScore != null && targetScore != null && targetScore > 0) {
                // 사용자 점수가 목표 점수에 얼마나 근접한지 계산
                double match = Math.min(userScore / targetScore, 1.0);
                totalMatch += match;
                count++;
            }
        }

        return count > 0 ? totalMatch / count : 0.0;
    }

    /**
     * 종합 요약 생성
     */
    private String generateSummary(
            NcsAnalysisResponse ncsAnalysis,
            List<KsaAnalysisResponse> ksaAnalyses,
            String careerLevel
    ) {
        StringBuilder summary = new StringBuilder();

        // 1. 커리어 레벨
        summary.append(String.format("현재 커리어 레벨: %s\n\n", careerLevel));

        // 2. 추천 직무
        if (!ncsAnalysis.candidates().isEmpty()) {
            NcsRecommendationCandidate topCandidate = ncsAnalysis.candidates().getFirst();
            summary.append(String.format("추천 직무: %s (%s)\n", topCandidate.ncsName(), topCandidate.ncsCode()));
            summary.append(String.format("적합도: %.1f%%\n\n", topCandidate.confidenceScore() * 100));
        }

        // 3. 역량 분석 요약
        if (!ksaAnalyses.isEmpty()) {
            KsaAnalysisResponse ksaAnalysis = ksaAnalyses.getFirst();
            summary.append("역량 분석:\n");
            summary.append(ksaAnalysis.overallAssessment());
            summary.append("\n\n");

            // 강점 분석
            List<String> strengths = findStrengths(ksaAnalysis);
            if (!strengths.isEmpty()) {
                summary.append("주요 강점:\n");
                strengths.forEach(strength -> summary.append("- ").append(strength).append("\n"));
                summary.append("\n");
            }

            // 개선 영역
            List<String> improvements = findImprovements(ksaAnalysis);
            if (!improvements.isEmpty()) {
                summary.append("개선 권장 영역:\n");
                improvements.forEach(improvement -> summary.append("- ").append(improvement).append("\n"));
                summary.append("\n");
            }
        }

        // 4. 커리어 전망 (커리어넷 정보 활용)
        if (!ncsAnalysis.candidates().isEmpty()) {
            NcsRecommendationCandidate topCandidate = ncsAnalysis.candidates().getFirst();
            if (topCandidate.careerNetJobInfo() != null) {
                CareerNetJobInfo jobInfo = topCandidate.careerNetJobInfo();
                if (jobInfo.prospect() != null && !jobInfo.prospect().isEmpty()) {
                    summary.append("직업 전망: ").append(jobInfo.prospect()).append("\n");
                }
                if (jobInfo.salaryLevel() != null && !jobInfo.salaryLevel().isEmpty()) {
                    summary.append("임금 수준: ").append(jobInfo.salaryLevel()).append("\n");
                }
            }
        }

        return summary.toString();
    }

    /**
     * 강점 항목 추출
     */
    private List<String> findStrengths(KsaAnalysisResponse ksaAnalysis) {
        List<String> strengths = new ArrayList<>();

        // Knowledge 강점
        ksaAnalysis.knowledgeItems().stream()
                .filter(item -> item.scoreGap() <= 0.1)
                .limit(2)
                .forEach(item -> strengths.add(item.itemName() + " (지식)"));

        // Skill 강점
        ksaAnalysis.skillItems().stream()
                .filter(item -> item.scoreGap() <= 0.1)
                .limit(2)
                .forEach(item -> strengths.add(item.itemName() + " (기술)"));

        // Attitude 강점
        ksaAnalysis.attitudeItems().stream()
                .filter(item -> item.scoreGap() <= 0.1)
                .limit(1)
                .forEach(item -> strengths.add(item.itemName() + " (태도)"));

        return strengths;
    }

    /**
     * 개선 영역 추출
     */
    private List<String> findImprovements(KsaAnalysisResponse ksaAnalysis) {
        List<String> improvements = new ArrayList<>();

        // Knowledge 개선 영역
        ksaAnalysis.knowledgeItems().stream()
                .filter(item -> item.scoreGap() > 0.2)
                .limit(2)
                .forEach(item -> improvements.add(item.itemName() + " (지식 보완 필요)"));

        // Skill 개선 영역
        ksaAnalysis.skillItems().stream()
                .filter(item -> item.scoreGap() > 0.2)
                .limit(2)
                .forEach(item -> improvements.add(item.itemName() + " (기술 향상 필요)"));

        // Attitude 개선 영역
        ksaAnalysis.attitudeItems().stream()
                .filter(item -> item.scoreGap() > 0.2)
                .limit(1)
                .forEach(item -> improvements.add(item.itemName() + " (태도 개선 필요)"));

        return improvements;
    }

    /**
     * 진행 상황을 SSE로 전송하는 헬퍼 메서드
     */
    private void reportProgress(DiagnosisContext context, int progressPercentage, String message) {
        if (context.getProgressCallback() != null) {
            DiagnosisProgressResponse progressResponse = DiagnosisProgressResponse.builder()
                    .diagnosisId(context.getDiagnosisId())
                    .currentStep(DiagnosisStep.FINAL_REPORT)
                    .progressPercentage(progressPercentage)
                    .status(DiagnosisStatus.IN_PROGRESS)
                    .currentMessage(message)
                    .build();

            context.getProgressCallback().accept(progressResponse);
            log.debug("Progress reported: {}% - {}", progressPercentage, message);
        }
    }

    @Override
    public String getName() {
        return "ReportGenerationProcessor";
    }
}
