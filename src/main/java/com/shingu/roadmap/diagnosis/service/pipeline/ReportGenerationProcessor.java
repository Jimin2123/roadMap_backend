package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.apis.careernet.dto.response.CareerNetIntegratedResponse;
import com.shingu.roadmap.apis.careernet.service.CareerNetIntegrationService;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.ncs.repository.NcsOccupationRepository;
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

    private final CareerNetIntegrationService careerNetIntegrationService;
    private final NcsOccupationRepository ncsOccupationRepository;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[ReportGenerationProcessor.process] ENTER - memberId: {}, diagnosisId: {}",
            context.getMemberId(), context.getDiagnosisId());
        long totalStartTime = System.currentTimeMillis();

        try {
            NcsAnalysisResponse ncsAnalysis = context.getNcsAnalysisResponse();
            List<KsaAnalysisResponse> ksaAnalyses = context.getKsaAnalysisResponses();
            String careerLevel = context.getCareerLevel();

            // Validate required inputs
            if (ncsAnalysis == null) {
                log.error("[ReportGenerationProcessor.process] NCS analysis result is missing - aborting");
                context.setSuccess(false);
                context.setErrorMessage("Required analysis results missing: NCS analysis is null");
                return context;
            }

            if (ksaAnalyses == null || ksaAnalyses.isEmpty()) {
                log.error("[ReportGenerationProcessor.process] KSA analysis results missing or empty - aborting");
                context.setSuccess(false);
                context.setErrorMessage("Required analysis results missing: KSA analysis is null or empty");
                return context;
            }
            log.debug("[ReportGenerationProcessor.process] Input data loaded - ncsCandidates: {}, ksaAnalyses: {}, careerLevel: {}",
                ncsAnalysis.candidates().size(), ksaAnalyses.size(), careerLevel);

            reportProgress(context, 66, "커리어넷 직업 정보를 조회하고 있습니다.");

            // 3-1. 커리어넷 직업 정보 조회 및 보강
            log.debug("[ReportGenerationProcessor.process] Starting CareerNet enrichment");
            long enrichStartTime = System.currentTimeMillis();
            NcsAnalysisResponse enrichedNcsAnalysis = enrichWithCareerNetInfo(ncsAnalysis, context);
            long enrichDuration = System.currentTimeMillis() - enrichStartTime;
            log.info("[ReportGenerationProcessor.process] CareerNet enrichment completed in {}ms", enrichDuration);

            reportProgress(context, 85, "레이더 차트 데이터를 생성하고 있습니다.");

            // 3-2. 레이더 차트 데이터 생성
            log.debug("[ReportGenerationProcessor.process] Generating radar chart data");
            long radarStartTime = System.currentTimeMillis();
            RadarChartData radarChartData = generateRadarChartData(ksaAnalyses);
            long radarDuration = System.currentTimeMillis() - radarStartTime;
            log.info("[ReportGenerationProcessor.process] Radar chart generated in {}ms - axes: {}, profiles: {}",
                radarDuration,
                radarChartData.competencyAxes() != null ? radarChartData.competencyAxes().size() : 0,
                radarChartData.targetNcsProfiles() != null ? radarChartData.targetNcsProfiles().size() : 0);

            reportProgress(context, 95, "종합 요약을 생성하고 있습니다.");

            // 3-3. 종합 요약 생성
            log.debug("[ReportGenerationProcessor.process] Generating summary");
            long summaryStartTime = System.currentTimeMillis();
            String summary = generateSummary(enrichedNcsAnalysis, ksaAnalyses, careerLevel);
            long summaryDuration = System.currentTimeMillis() - summaryStartTime;
            log.info("[ReportGenerationProcessor.process] Summary generated in {}ms - length: {}",
                summaryDuration, summary.length());

            // 3-4. 최종 리포트 생성 (채용공고 및 자격증 추천 포함)
            log.debug("[ReportGenerationProcessor.process] Building final diagnosis result");
            DiagnosisResultResponse diagnosisResult = DiagnosisResultResponse.builder()
                    .diagnosisId(null) // 실제 저장 시 할당
                    .summary(summary)
                    .ncsAnalyses(List.of(enrichedNcsAnalysis))
                    .confidenceScore(enrichedNcsAnalysis.overallConfidence())
                    .radarChartData(radarChartData)
                    .jobRecommendations(context.getJobRecommendations())
                    .certificationRecommendations(context.getCertificationRecommendations())
                    .build();

            log.info("[ReportGenerationProcessor.process] Diagnosis result includes {} job recommendations and {} certification recommendations",
                    context.getJobRecommendations() != null ? context.getJobRecommendations().size() : 0,
                    context.getCertificationRecommendations() != null ? context.getCertificationRecommendations().size() : 0);

            context.setDiagnosisResultResponse(diagnosisResult);
            context.setSuccess(true);

            reportProgress(context, 100, "진단 리포트가 완료되었습니다.");

            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.info("[ReportGenerationProcessor.process] EXIT (SUCCESS) - totalDuration: {}ms, confidenceScore: {}, summaryLength: {}",
                    totalDuration, enrichedNcsAnalysis.overallConfidence(), summary.length());

            return context;

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.error("[ReportGenerationProcessor.process] EXCEPTION - memberId: {}, totalDuration: {}ms, error: {}",
                context.getMemberId(), totalDuration, e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("리포트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return context;
        }
    }

    /**
     * 커리어넷 통합 정보 및 KSA 분석 결과로 NCS 분석 결과 보강
     */
    private NcsAnalysisResponse enrichWithCareerNetInfo(NcsAnalysisResponse ncsAnalysis, DiagnosisContext context) {
        List<NcsRecommendationCandidate> enrichedCandidates = new ArrayList<>();
        List<NcsRecommendationCandidate> candidates = ncsAnalysis.candidates();
        List<KsaAnalysisResponse> ksaAnalyses = context.getKsaAnalysisResponses();
        int totalCandidates = candidates.size();
        int processedCount = 0;

        // NCS 코드별 KSA 분석 결과 매핑
        Map<String, KsaAnalysisResponse> ksaByNcsCode = new HashMap<>();
        if (ksaAnalyses != null) {
            for (KsaAnalysisResponse ksaAnalysis : ksaAnalyses) {
                ksaByNcsCode.put(ksaAnalysis.ncsCode(), ksaAnalysis);
            }
        }

        for (NcsRecommendationCandidate candidate : candidates) {
            try {
                // 1. NCS Occupation 조회
                NcsOccupation ncsOccupation = ncsOccupationRepository.findById(candidate.ncsCode()).orElse(null);

                if (ncsOccupation == null) {
                    log.warn("NCS occupation not found for code: {}", candidate.ncsCode());
                    enrichedCandidates.add(candidate);
                    processedCount++;
                    continue;
                }

                // 2. 커리어넷 통합 정보 조회 (Job Info, Encyclopedia, Counseling Cases)
                CareerNetIntegratedResponse integratedResponse = careerNetIntegrationService
                        .getIntegratedCareerInfo(ncsOccupation)
                        .block(java.time.Duration.ofSeconds(30)); // Timeout for CareerNet API

                CareerNetJobInfo careerNetJobInfo = null;

                // 3. Job Info 추출 및 매핑
                if (integratedResponse != null && integratedResponse.jobInfoDetail() != null
                        && integratedResponse.jobInfoDetail().dataSearch() != null
                        && integratedResponse.jobInfoDetail().dataSearch().content() != null
                        && !integratedResponse.jobInfoDetail().dataSearch().content().isEmpty()) {
                    var jobInfo = integratedResponse.jobInfoDetail().dataSearch().content().getFirst();

                    // 직업 전망
                    String prospect = (jobInfo.jobPossibility() != null && !jobInfo.jobPossibility().isEmpty()) ?
                            jobInfo.jobPossibility().getFirst().possibility() : null;

                    // 취업 현황 정보 추출
                    String employmentMethod = null;
                    String employmentStatus = null;
                    String salaryLevel = null;
                    if (jobInfo.stateOfEmployment() != null) {
                        for (var emp : jobInfo.stateOfEmployment()) {
                            if (emp.empWay() != null && !emp.empWay().isEmpty()) {
                                employmentMethod = emp.empWay();
                            }
                            if (emp.employment() != null && !emp.employment().isEmpty()) {
                                employmentStatus = emp.employment();
                            }
                            if (emp.salary() != null && !emp.salary().isEmpty()) {
                                salaryLevel = emp.salary();
                            }
                        }
                    }

                    // 준비 방법 정보 추출
                    String educationPath = null;
                    String trainingInfo = null;
                    String relatedCertifications = null;
                    if (jobInfo.preparationWay() != null) {
                        for (var prep : jobInfo.preparationWay()) {
                            if (prep.preparation() != null && !prep.preparation().isEmpty()) {
                                educationPath = prep.preparation();
                            }
                            if (prep.training() != null && !prep.training().isEmpty()) {
                                trainingInfo = prep.training();
                            }
                            if (prep.certification() != null && !prep.certification().isEmpty()) {
                                relatedCertifications = prep.certification();
                            }
                        }
                    }

                    // 관련 학과 추출
                    List<String> relatedMajors = null;
                    if (jobInfo.capacityMajor() != null && !jobInfo.capacityMajor().isEmpty()) {
                        var capacityMajor = jobInfo.capacityMajor().getFirst();
                        if (capacityMajor.majors() != null) {
                            relatedMajors = capacityMajor.majors().stream()
                                    .map(major -> major.majorName())
                                    .filter(name -> name != null && !name.isEmpty())
                                    .toList();
                        }
                    }

                    careerNetJobInfo = CareerNetJobInfo.builder()
                            .jobName(jobInfo.job())
                            .summary(jobInfo.summary())
                            .coreAbilities(jobInfo.ability())
                            .aptitudeAndInterest(jobInfo.aptitude())
                            .similarJobs(jobInfo.similarJob())
                            .relatedCertifications(relatedCertifications)
                            .relatedMajors(relatedMajors != null && !relatedMajors.isEmpty() ? relatedMajors : null)
                            .employmentMethod(employmentMethod)
                            .employmentStatus(employmentStatus)
                            .salaryLevel(salaryLevel)
                            .prospect(prospect)
                            .educationPath(educationPath)
                            .trainingInfo(trainingInfo)
                            .build();

                    log.debug("CareerNet info fetched successfully for NCS code {} with {} fields populated",
                            candidate.ncsCode(),
                            countNonNullFields(careerNetJobInfo));
                }

                // 4. Counseling Cases 추출 및 변환
                List<CounselingCaseInfo> counselingCases = null;
                if (integratedResponse != null && integratedResponse.counselingCases() != null
                        && !integratedResponse.counselingCases().isEmpty()) {
                    counselingCases = integratedResponse.counselingCases().stream()
                            .map(enrichedCase -> CounselingCaseInfo.builder()
                                    .title(enrichedCase.memo() != null ? enrichedCase.memo() : "상담 사례")
                                    .question(enrichedCase.question())
                                    .answer(enrichedCase.answer())
                                    .category(enrichedCase.gubun())
                                    .build())
                            .toList();

                    log.debug("Counseling cases fetched successfully for NCS code {}: {} cases",
                            candidate.ncsCode(), counselingCases.size());
                }

                // 5. 해당 NCS 코드에 대한 KSA 분석 결과 찾기
                KsaAnalysisResponse ksaAnalysis = ksaByNcsCode.get(candidate.ncsCode());

                // 6. 후보 정보에 커리어넷 정보, 상담 사례 및 KSA 분석 결과 추가
                enrichedCandidates.add(candidate.toBuilder()
                        .careerNetJobInfo(careerNetJobInfo)
                        .counselingCases(counselingCases)
                        .ksaAnalysis(ksaAnalysis)
                        .build());

            } catch (Exception e) {
                log.warn("Failed to enrich candidate for NCS code {}: {}", candidate.ncsCode(), e.getMessage());
                // 오류 발생 시에도 원본 candidate는 유지
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

        // 1. 공통 역량 축 생성 (itemDescription 사용 - 실제 역량명)
        List<String> competencyAxes = new ArrayList<>();
        competencyAxes.addAll(ksaAnalysis.knowledgeItems().stream()
                .map(item -> cleanCompetencyName(item.itemDescription()))
                .limit(5)
                .toList());
        competencyAxes.addAll(ksaAnalysis.skillItems().stream()
                .map(item -> cleanCompetencyName(item.itemDescription()))
                .limit(5)
                .toList());
        competencyAxes.addAll(ksaAnalysis.attitudeItems().stream()
                .map(item -> cleanCompetencyName(item.itemDescription()))
                .limit(3)
                .toList());

        // 2. 사용자 역량 프로필 생성
        Map<String, Double> userCompetencyScores = new HashMap<>();

        // Knowledge 점수
        ksaAnalysis.knowledgeItems().stream()
                .limit(5)
                .forEach(item -> userCompetencyScores.put(
                        cleanCompetencyName(item.itemDescription()),
                        item.userScore()));

        // Skill 점수
        ksaAnalysis.skillItems().stream()
                .limit(5)
                .forEach(item -> userCompetencyScores.put(
                        cleanCompetencyName(item.itemDescription()),
                        item.userScore()));

        // Attitude 점수
        ksaAnalysis.attitudeItems().stream()
                .limit(3)
                .forEach(item -> userCompetencyScores.put(
                        cleanCompetencyName(item.itemDescription()),
                        item.userScore()));

        CompetencyProfile userProfile = CompetencyProfile.builder()
                .profileName("현재 보유 역량")
                .competencyScores(userCompetencyScores)
                .build();

        // 3. 목표 NCS 역량 프로필 생성
        Map<String, Double> targetCompetencyScores = new HashMap<>();

        // Knowledge 목표 점수
        ksaAnalysis.knowledgeItems().stream()
                .limit(5)
                .forEach(item -> targetCompetencyScores.put(
                        cleanCompetencyName(item.itemDescription()),
                        item.targetScore()));

        // Skill 목표 점수
        ksaAnalysis.skillItems().stream()
                .limit(5)
                .forEach(item -> targetCompetencyScores.put(
                        cleanCompetencyName(item.itemDescription()),
                        item.targetScore()));

        // Attitude 목표 점수
        ksaAnalysis.attitudeItems().stream()
                .limit(3)
                .forEach(item -> targetCompetencyScores.put(
                        cleanCompetencyName(item.itemDescription()),
                        item.targetScore()));

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
     * 역량명 정리 (번호 접두사 제거)
     * 예: "1.유스케이스" → "유스케이스"
     */
    private String cleanCompetencyName(String itemDescription) {
        if (itemDescription == null || itemDescription.isEmpty()) {
            return itemDescription;
        }

        // "1.", "2." 같은 번호 접두사 제거
        return itemDescription.replaceFirst("^\\d+\\.", "").trim();
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
     * CareerNetJobInfo의 non-null 필드 개수 카운트
     */
    private int countNonNullFields(CareerNetJobInfo jobInfo) {
        if (jobInfo == null) return 0;

        int count = 0;
        if (jobInfo.jobName() != null) count++;
        if (jobInfo.summary() != null) count++;
        if (jobInfo.coreAbilities() != null) count++;
        if (jobInfo.aptitudeAndInterest() != null) count++;
        if (jobInfo.similarJobs() != null) count++;
        if (jobInfo.relatedCertifications() != null) count++;
        if (jobInfo.relatedMajors() != null) count++;
        if (jobInfo.employmentMethod() != null) count++;
        if (jobInfo.employmentStatus() != null) count++;
        if (jobInfo.salaryLevel() != null) count++;
        if (jobInfo.prospect() != null) count++;
        if (jobInfo.educationPath() != null) count++;
        if (jobInfo.trainingInfo() != null) count++;

        return count;
    }

    /**
     * 진행 상황을 SSE로 전송하는 헬퍼 메서드
     */
    private void reportProgress(DiagnosisContext context, int progressPercentage, String message) {
        log.debug("[ReportGenerationProcessor.reportProgress] ENTER - diagnosisId: {}, percentage: {}%, message: {}",
            context.getDiagnosisId(), progressPercentage, message);

        if (context.getProgressCallback() != null) {
            try {
                DiagnosisProgressResponse progressResponse = DiagnosisProgressResponse.builder()
                        .diagnosisId(context.getDiagnosisId())
                        .currentStep(DiagnosisStep.FINAL_REPORT)
                        .progressPercentage(progressPercentage)
                        .status(DiagnosisStatus.IN_PROGRESS)
                        .currentMessage(message)
                        .build();

                context.getProgressCallback().accept(progressResponse);
                log.debug("[ReportGenerationProcessor.reportProgress] EXIT - progress sent successfully");
            } catch (Exception e) {
                log.error("[ReportGenerationProcessor.reportProgress] EXCEPTION - diagnosisId: {}, error: {}",
                    context.getDiagnosisId(), e.getMessage(), e);
            }
        } else {
            log.warn("[ReportGenerationProcessor.reportProgress] No progress callback available - diagnosisId: {}",
                context.getDiagnosisId());
        }
    }

    @Override
    public String getName() {
        return "ReportGenerationProcessor";
    }
}
