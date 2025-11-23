package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.apis.ncs.dto.response.NcsJobPositionResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsKsaResponse;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.apis.openai.service.workflow.NcsCompetencyAnalysisWorkflow;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.diagnosis.dto.common.Evidence;
import com.shingu.roadmap.diagnosis.dto.common.EvidenceSourceType;
import com.shingu.roadmap.diagnosis.dto.common.NcsRecommendationCandidate;
import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.resume.domain.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 2단계: 목표 직무 기반 역량 및 커리어 레벨 진단 프로세서
 *
 * 처리 흐름:
 * 2-1. 확정된 NCS 코드로 KSA(Knowledge, Skill, Attitude) 요구사항 조회
 * 2-2. 사용자 보유 역량과 목표 역량 비교 분석
 * 2-3. 커리어 레벨 진단 (경력 기간, 프로젝트 역할, 기술 숙련도 종합)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompetencyAnalysisProcessor implements DiagnosisProcessor {

    private final NcsApiService ncsApiService;
    private final OpenAiService openAiService;
    private final ResumeTextFormatter resumeTextFormatter;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[CompetencyAnalysisProcessor.process] ENTER - memberId: {}, diagnosisId: {}",
            context.getMemberId(), context.getDiagnosisId());
        long totalStartTime = System.currentTimeMillis();

        try {
            NcsAnalysisResponse ncsAnalysis = context.getNcsAnalysisResponse();
            if (ncsAnalysis == null || ncsAnalysis.candidates() == null || ncsAnalysis.candidates().isEmpty()) {
                long duration = System.currentTimeMillis() - totalStartTime;
                log.error("[CompetencyAnalysisProcessor.process] NCS analysis result is missing or empty - aborting, duration: {}ms", duration);
                context.setSuccess(false);
                context.setErrorMessage("Required analysis results missing: NCS analysis is null or empty");
                log.info("[CompetencyAnalysisProcessor.process] EXIT (NCS ANALYSIS MISSING) - duration: {}ms", duration);
                return context;
            }
            log.debug("[CompetencyAnalysisProcessor.process] NCS analysis loaded - candidatesCount: {}",
                ncsAnalysis.candidates().size());

            Profile profile = context.getProfile();

            // 2-1. 모든 후보에 대해 KSA 분석 수행
            log.info("[CompetencyAnalysisProcessor.process] Performing KSA analysis for {} candidates",
                    ncsAnalysis.candidates().size());
            long ksaStartTime = System.currentTimeMillis();

            List<KsaAnalysisResponse> allKsaAnalysisResponses = new ArrayList<>();
            int candidateIndex = 0;
            int totalCandidates = ncsAnalysis.candidates().size();

            for (NcsRecommendationCandidate candidate : ncsAnalysis.candidates()) {
                candidateIndex++;
                log.info("[CompetencyAnalysisProcessor.process] Analyzing candidate {}/{} - ncsCode: {}",
                        candidateIndex, totalCandidates, candidate.ncsCode());

                try {
                    List<KsaAnalysisResponse> candidateKsaResponses = performKsaAnalysis(
                            candidate.ncsCode(), profile, context);

                    if (!candidateKsaResponses.isEmpty()) {
                        allKsaAnalysisResponses.addAll(candidateKsaResponses);
                        log.debug("[CompetencyAnalysisProcessor.process] KSA analysis successful for ncsCode: {} - {} responses",
                                candidate.ncsCode(), candidateKsaResponses.size());
                    } else {
                        log.warn("[CompetencyAnalysisProcessor.process] KSA analysis returned empty for ncsCode: {}",
                                candidate.ncsCode());
                    }
                } catch (Exception e) {
                    log.error("[CompetencyAnalysisProcessor.process] KSA analysis failed for ncsCode: {} - error: {}",
                            candidate.ncsCode(), e.getMessage(), e);
                    // 개별 후보 실패는 무시하고 계속 진행
                }

                // 진행률 업데이트 (40% ~ 60% 범위)
                int progress = 40 + (20 * candidateIndex / totalCandidates);
                reportProgress(context, progress,
                        String.format("역량 분석 중... (%d/%d)", candidateIndex, totalCandidates));
            }

            long ksaDuration = System.currentTimeMillis() - ksaStartTime;
            log.info("[CompetencyAnalysisProcessor.process] All KSA analyses completed in {}ms - totalResponses: {}",
                    ksaDuration, allKsaAnalysisResponses.size());

            if (allKsaAnalysisResponses.isEmpty()) {
                long duration = System.currentTimeMillis() - totalStartTime;
                log.warn("[CompetencyAnalysisProcessor.process] No KSA analysis results for any candidate - duration: {}ms",
                        duration);
                context.setSuccess(false);
                context.setErrorMessage("KSA 역량 분석에 실패했습니다.");
                log.info("[CompetencyAnalysisProcessor.process] EXIT (ALL KSA FAILED) - duration: {}ms", duration);
                return context;
            }

            reportProgress(context, 63, "커리어 레벨을 진단하고 있습니다.");

            // 2-2. 타겟 NCS 코드 결정 (커리어 레벨 진단용)
            log.debug("[CompetencyAnalysisProcessor.process] Determining target NCS code for career level diagnosis");
            String targetNcsCode = determineTargetNcsCode(context);

            if (targetNcsCode == null) {
                long duration = System.currentTimeMillis() - totalStartTime;
                log.warn("[CompetencyAnalysisProcessor.process] No target NCS code available for career level - duration: {}ms", duration);
                // KSA 분석은 완료되었으므로 커리어 레벨만 기본값 사용
                context.setKsaAnalysisResponses(allKsaAnalysisResponses);
                context.setCareerLevel("신입 / 초급"); // 기본값
                context.setSuccess(true);
                reportProgress(context, 66, "역량 분석이 완료되었습니다.");
                return context;
            }

            log.info("[CompetencyAnalysisProcessor.process] Target NCS code determined: {} (userSelected: {})",
                    targetNcsCode, context.getUserSelectedNcsCode() != null);

            // 2-3. 커리어 레벨 진단
            log.debug("[CompetencyAnalysisProcessor.process] Starting career level diagnosis");
            long careerStartTime = System.currentTimeMillis();
            String careerLevel = diagnoseCareerLevel(targetNcsCode, profile);
            long careerDuration = System.currentTimeMillis() - careerStartTime;
            log.info("[CompetencyAnalysisProcessor.process] Career level diagnosed in {}ms - level: {}",
                careerDuration, careerLevel);

            context.setKsaAnalysisResponses(allKsaAnalysisResponses);
            context.setCareerLevel(careerLevel);
            context.setSuccess(true);

            reportProgress(context, 66, "역량 분석이 완료되었습니다.");

            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.info("[CompetencyAnalysisProcessor.process] EXIT (SUCCESS) - totalDuration: {}ms, targetNcsCode: {}, careerLevel: {}, ksaResponsesCount: {}",
                    totalDuration, targetNcsCode, careerLevel, allKsaAnalysisResponses.size());

            return context;

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.error("[CompetencyAnalysisProcessor.process] EXCEPTION - memberId: {}, totalDuration: {}ms, error: {}",
                context.getMemberId(), totalDuration, e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("역량 분석 중 오류가 발생했습니다: " + e.getMessage());
            return context;
        }
    }

    /**
     * 분석 대상 NCS 코드 결정
     * (사용자 선택 > 자동 선택 > 첫 번째 후보)
     */
    private String determineTargetNcsCode(DiagnosisContext context) {
        // 사용자가 직접 선택한 경우
        if (context.getUserSelectedNcsCode() != null) {
            return context.getUserSelectedNcsCode();
        }

        // 자동 선택된 경우
        NcsAnalysisResponse ncsAnalysis = context.getNcsAnalysisResponse();
        if (ncsAnalysis.selectedNcsCode() != null) {
            return ncsAnalysis.selectedNcsCode();
        }

        // 첫 번째 후보 선택
        if (!ncsAnalysis.candidates().isEmpty()) {
            return ncsAnalysis.candidates().getFirst().ncsCode();
        }

        return null;
    }

    /**
     * KSA 역량 분석 수행 (AI 기반)
     */
    private List<KsaAnalysisResponse> performKsaAnalysis(String ncsCode, Profile profile, DiagnosisContext context) {
        List<KsaAnalysisResponse> responses = new ArrayList<>();

        try {
            reportProgress(context, 33, "능력단위 정보를 조회하고 있습니다.");

            // 능력단위 코드 목록 조회 (임시 - 첫 번째 능력단위만 사용)
            var compUnitResponse = ncsApiService.getNcsCompUnit(ncsCode);
            if (compUnitResponse == null || compUnitResponse.data() == null || compUnitResponse.data().isEmpty()) {
                log.warn("No competency units found for NCS code: {}", ncsCode);
                return responses;
            }

            String firstCompUnitCd = compUnitResponse.data().getFirst().compUnitCd();

            reportProgress(context, 38, "KSA 데이터를 조회하고 있습니다.");

            // KSA 데이터 조회
            NcsKsaResponse ksaResponse = ncsApiService.getNcsKsa(ncsCode, firstCompUnitCd);
            if (ksaResponse == null || ksaResponse.data() == null) {
                log.warn("No KSA data found for NCS code: {}", ncsCode);
                return responses;
            }

            reportProgress(context, 43, "KSA 항목을 분류하고 있습니다.");

            // KSA 항목 분류
            List<NcsKsaResponse.NcsKsaItem> knowledgeRawItems = ksaResponse.data().stream()
                    .filter(item -> item.gbnName() != null && item.gbnName().contains("지식"))
                    .collect(Collectors.toList());

            List<NcsKsaResponse.NcsKsaItem> skillRawItems = ksaResponse.data().stream()
                    .filter(item -> item.gbnName() != null && item.gbnName().contains("기술"))
                    .collect(Collectors.toList());

            List<NcsKsaResponse.NcsKsaItem> attitudeRawItems = ksaResponse.data().stream()
                    .filter(item -> item.gbnName() != null && item.gbnName().contains("태도"))
                    .collect(Collectors.toList());

            // AI 기반 KSA 분석 수행
            reportProgress(context, 43, "지식(Knowledge) 역량을 AI로 분석하고 있습니다.");
            List<KsaAnalysisResponse.KsaItem> knowledgeItems = analyzeKsaCategoryWithAi(
                    ncsCode, knowledgeRawItems, profile, "지식"
            );

            reportProgress(context, 50, "기술(Skill) 역량을 AI로 분석하고 있습니다.");
            List<KsaAnalysisResponse.KsaItem> skillItems = analyzeKsaCategoryWithAi(
                    ncsCode, skillRawItems, profile, "기술"
            );

            reportProgress(context, 57, "태도(Attitude) 역량을 AI로 분석하고 있습니다.");
            List<KsaAnalysisResponse.KsaItem> attitudeItems = analyzeKsaCategoryWithAi(
                    ncsCode, attitudeRawItems, profile, "태도"
            );

            // 전체 평가 요약
            String overallAssessment = generateOverallAssessment(knowledgeItems, skillItems, attitudeItems);

            // 근거 목록 생성
            List<Evidence> evidenceList = generateKsaEvidenceList(profile);

            KsaAnalysisResponse ksaAnalysis = KsaAnalysisResponse.builder()
                    .ncsCode(ncsCode)
                    .knowledgeItems(knowledgeItems)
                    .skillItems(skillItems)
                    .attitudeItems(attitudeItems)
                    .overallAssessment(overallAssessment)
                    .evidenceList(evidenceList)
                    .build();

            responses.add(ksaAnalysis);

        } catch (Exception e) {
            log.error("Failed to perform KSA analysis: {}", e.getMessage(), e);
        }

        return responses;
    }

    /**
     * AI 기반 KSA 카테고리별 항목 분석
     */
    private List<KsaAnalysisResponse.KsaItem> analyzeKsaCategoryWithAi(
            String ncsCode,
            List<NcsKsaResponse.NcsKsaItem> ksaItems,
            Profile profile,
            String categoryName
    ) {
        if (ksaItems.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // KSA 항목명 추출
            List<String> itemNames = ksaItems.stream()
                    .map(NcsKsaResponse.NcsKsaItem::gbnName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (itemNames.isEmpty()) {
                log.warn("No valid KSA item names for category: {}", categoryName);
                return analyzeKsaCategoryFallback(ksaItems, profile);
            }

            // AI 분석 호출
            Map<String, NcsCompetencyAnalysisWorkflow.KsaEvaluationResult> aiResults;
            try {
                aiResults = openAiService.analyzeKsaCompetency(ncsCode, itemNames, profile).block();
            } catch (Exception e) {
                log.error("[CompetencyAnalysisProcessor] AI service error during KSA analysis for category {}: {}",
                        categoryName, e.getMessage(), e);
                log.warn("[CompetencyAnalysisProcessor] Falling back to rule-based analysis due to AI service error");
                return analyzeKsaCategoryFallback(ksaItems, profile);
            }

            // AI 결과가 비어있으면 fallback 사용 (legitimately no results)
            if (aiResults == null || aiResults.isEmpty()) {
                log.warn("[CompetencyAnalysisProcessor] AI returned empty results for category: {} (not an error), using fallback",
                        categoryName);
                return analyzeKsaCategoryFallback(ksaItems, profile);
            }

            // AI 결과를 KsaItem으로 매핑
            List<KsaAnalysisResponse.KsaItem> result = ksaItems.stream()
                    .map(rawItem -> {
                        String itemName = rawItem.gbnName();
                        NcsCompetencyAnalysisWorkflow.KsaEvaluationResult aiResult = aiResults.get(itemName);

                        if (aiResult == null) {
                            // AI 결과가 없는 항목은 기본 분석 사용
                            log.debug("No AI result for item: {}, using basic calculation", itemName);
                            return createKsaItemFallback(rawItem, profile);
                        }

                        // AI 결과로 KsaItem 생성
                        double targetScore = 0.8; // 목표 수준
                        double scoreGap = targetScore - aiResult.userScore();

                        return KsaAnalysisResponse.KsaItem.builder()
                                .itemName(itemName)
                                .itemDescription(rawItem.gbnVal())
                                .userScore(aiResult.userScore())
                                .targetScore(targetScore)
                                .scoreGap(scoreGap)
                                .levelAssessment(aiResult.levelAssessment())
                                .gapDescription(aiResult.gapDescription())
                                .recommendation(aiResult.recommendation())
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("Successfully analyzed {} {} items using AI", result.size(), categoryName);
            return result;

        } catch (Exception e) {
            log.error("Failed to analyze KSA category {} with AI: {}", categoryName, e.getMessage(), e);
            return analyzeKsaCategoryFallback(ksaItems, profile);
        }
    }

    /**
     * Fallback: 기본 키워드 매칭 기반 KSA 분석
     */
    private List<KsaAnalysisResponse.KsaItem> analyzeKsaCategoryFallback(
            List<NcsKsaResponse.NcsKsaItem> ksaItems,
            Profile profile
    ) {
        return ksaItems.stream()
                .map(item -> createKsaItemFallback(item, profile))
                .collect(Collectors.toList());
    }

    /**
     * 단일 KSA 항목의 fallback 분석
     */
    private KsaAnalysisResponse.KsaItem createKsaItemFallback(
            NcsKsaResponse.NcsKsaItem item,
            Profile profile
    ) {
        double userScore = calculateUserScore(item.gbnName(), profile);
        double targetScore = 0.8;
        double scoreGap = targetScore - userScore;
        String levelAssessment = assessLevel(scoreGap);
        String gapDescription = generateGapDescription(item.gbnName(), scoreGap);
        String recommendation = generateRecommendation(item.gbnName(), scoreGap);

        return KsaAnalysisResponse.KsaItem.builder()
                .itemName(item.gbnName())
                .itemDescription(item.gbnVal())
                .userScore(userScore)
                .targetScore(targetScore)
                .scoreGap(scoreGap)
                .levelAssessment(levelAssessment)
                .gapDescription(gapDescription)
                .recommendation(recommendation)
                .build();
    }

    /**
     * [DEPRECATED] KSA 카테고리별 항목 분석 - 기본 방식 (fallback용으로만 사용)
     */
    @Deprecated
    private List<KsaAnalysisResponse.KsaItem> analyzeKsaCategory(
            List<NcsKsaResponse.NcsKsaItem> ksaItems,
            Profile profile
    ) {
        return ksaItems.stream()
                .map(item -> {
                    // 사용자 보유 수준 계산
                    double userScore = calculateUserScore(item.gbnName(), profile);

                    // 목표 수준 (NCS 표준)
                    double targetScore = 0.8; // 기본 목표 수준

                    // 점수 갭
                    double scoreGap = targetScore - userScore;

                    // 수준 평가
                    String levelAssessment = assessLevel(scoreGap);

                    // 갭 설명
                    String gapDescription = generateGapDescription(item.gbnName(), scoreGap);

                    // 추천 사항
                    String recommendation = generateRecommendation(item.gbnName(), scoreGap);

                    return KsaAnalysisResponse.KsaItem.builder()
                            .itemName(item.gbnName())
                            .itemDescription(item.gbnVal())
                            .userScore(userScore)
                            .targetScore(targetScore)
                            .scoreGap(scoreGap)
                            .levelAssessment(levelAssessment)
                            .gapDescription(gapDescription)
                            .recommendation(recommendation)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자 보유 수준 점수 계산
     */
    private double calculateUserScore(String ksaName, Profile profile) {
        double score = 0.5; // 기본 점수

        // 스킬 매칭
        long matchingSkills = profile.getProfileSkills().stream()
                .filter(ps -> ksaName.contains(ps.getSkill().getName()) ||
                        ps.getSkill().getName().contains(ksaName))
                .count();

        score += Math.min(0.3, matchingSkills * 0.15);

        // 프로젝트 경험 매칭
        if (profile.getResume() != null && profile.getResume().getProjects() != null) {
            String resumeText = resumeTextFormatter
                    .resumeToText(profile.getResume()).toLowerCase();
            if (resumeText.contains(ksaName.toLowerCase())) {
                score += 0.2;
            }
        }

        return Math.min(1.0, score);
    }

    /**
     * 수준 평가
     */
    private String assessLevel(double scoreGap) {
        if (scoreGap <= 0) return "EXCELLENT";
        if (scoreGap <= 0.1) return "ADEQUATE";
        if (scoreGap <= 0.3) return "NEED_IMPROVEMENT";
        return "INSUFFICIENT";
    }

    /**
     * 갭 설명 생성
     */
    private String generateGapDescription(String ksaName, double scoreGap) {
        if (scoreGap <= 0) {
            return String.format("%s 영역에서 우수한 역량을 보유하고 있습니다.", ksaName);
        } else if (scoreGap <= 0.1) {
            return String.format("%s 영역에서 적정 수준의 역량을 보유하고 있습니다.", ksaName);
        } else if (scoreGap <= 0.3) {
            return String.format("%s 영역에서 추가 학습이 필요합니다.", ksaName);
        } else {
            return String.format("%s 영역에서 상당한 역량 강화가 필요합니다.", ksaName);
        }
    }

    /**
     * 추천 사항 생성
     */
    private String generateRecommendation(String ksaName, double scoreGap) {
        if (scoreGap <= 0.1) {
            return "현재 수준을 유지하며 실무 경험을 쌓으세요.";
        } else if (scoreGap <= 0.3) {
            return String.format("%s 관련 온라인 강좌나 서적을 통해 학습하세요.", ksaName);
        } else {
            return String.format("%s 분야의 체계적인 교육 과정 이수를 권장합니다.", ksaName);
        }
    }

    /**
     * 전체 평가 요약 생성
     */
    private String generateOverallAssessment(
            List<KsaAnalysisResponse.KsaItem> knowledgeItems,
            List<KsaAnalysisResponse.KsaItem> skillItems,
            List<KsaAnalysisResponse.KsaItem> attitudeItems
    ) {
        double avgKnowledgeGap = knowledgeItems.stream()
                .mapToDouble(KsaAnalysisResponse.KsaItem::scoreGap)
                .average()
                .orElse(0.0);

        double avgSkillGap = skillItems.stream()
                .mapToDouble(KsaAnalysisResponse.KsaItem::scoreGap)
                .average()
                .orElse(0.0);

        double avgAttitudeGap = attitudeItems.stream()
                .mapToDouble(KsaAnalysisResponse.KsaItem::scoreGap)
                .average()
                .orElse(0.0);

        StringBuilder assessment = new StringBuilder();

        if (avgKnowledgeGap < 0.2 && avgSkillGap < 0.2 && avgAttitudeGap < 0.2) {
            assessment.append("전반적으로 해당 직무에 적합한 역량을 보유하고 있습니다. ");
        } else {
            assessment.append("해당 직무 수행을 위해 일부 역량 강화가 필요합니다. ");
        }

        if (avgSkillGap > avgKnowledgeGap && avgSkillGap > avgAttitudeGap) {
            assessment.append("특히 실무 기술(Skill) 영역의 보완이 중요합니다.");
        } else if (avgKnowledgeGap > avgSkillGap && avgKnowledgeGap > avgAttitudeGap) {
            assessment.append("특히 전문 지식(Knowledge) 영역의 학습이 필요합니다.");
        } else {
            assessment.append("균형 잡힌 역량 개발을 추천합니다.");
        }

        return assessment.toString();
    }

    /**
     * KSA 분석 근거 목록 생성
     */
    private List<Evidence> generateKsaEvidenceList(Profile profile) {
        List<Evidence> evidences = new ArrayList<>();

        // 스킬 근거
        profile.getProfileSkills().stream()
                .limit(3)
                .forEach(ps -> evidences.add(Evidence.builder()
                        .sourceType(EvidenceSourceType.SKILL)
                        .sourceDetail("보유 기술")
                        .content(ps.getSkill().getName() + " - " + ps.getProficiency())
                        .reasoning("기술 역량 보유")
                        .build()));

        // 프로젝트 근거
        if (profile.getResume() != null && profile.getResume().getProjects() != null) {
            profile.getResume().getProjects().stream()
                    .limit(2)
                    .forEach(project -> evidences.add(Evidence.builder()
                            .sourceType(EvidenceSourceType.PROJECT)
                            .sourceDetail("프로젝트 경험")
                            .content(project.getName() + " - " + project.getRole())
                            .reasoning("실무 경험")
                            .build()));
        }

        return evidences;
    }

    /**
     * 커리어 레벨 진단
     */
    private String diagnoseCareerLevel(String ncsCode, Profile profile) {
        try {
            // NCS 직책 정보 조회
            NcsJobPositionResponse jobPositionResponse = ncsApiService.getNcsJobPosition(ncsCode);

            // 경력 기간 계산
            int totalExperienceMonths = calculateTotalExperience(profile);
            int experienceYears = totalExperienceMonths / 12;

            // 프로젝트 역할 분석
            String projectRole = analyzeProjectRole(profile);

            // 기술 숙련도 분석
            double avgProficiencyLevel = calculateAverageProficiency(profile);

            // 커리어 레벨 결정
            String careerLevel = determineCareerLevel(
                    experienceYears,
                    projectRole,
                    avgProficiencyLevel,
                    jobPositionResponse
            );

            log.info("Career level diagnosed: {} (Experience: {} years, Role: {}, Proficiency: {})",
                    careerLevel, experienceYears, projectRole, avgProficiencyLevel);

            return careerLevel;

        } catch (Exception e) {
            log.error("Failed to diagnose career level: {}", e.getMessage(), e);
            return "ENTRY_LEVEL";
        }
    }

    /**
     * 총 경력 기간 계산 (개월)
     */
    private int calculateTotalExperience(Profile profile) {
        if (profile.getResume() == null || profile.getResume().getProjects() == null) {
            return 0;
        }

        return profile.getResume().getProjects().stream()
                .filter(project -> project.getPeriod() != null && project.getPeriod().getStartDate() != null)
                .mapToInt(project -> {
                    LocalDate start = project.getPeriod().getStartDate();
                    LocalDate end = project.getPeriod().getEndDate() != null
                            ? project.getPeriod().getEndDate()
                            : LocalDate.now();
                    return Period.between(start, end).getYears() * 12 + Period.between(start, end).getMonths();
                })
                .sum();
    }

    /**
     * 프로젝트 역할 분석
     */
    private String analyzeProjectRole(Profile profile) {
        if (profile.getResume() == null || profile.getResume().getProjects() == null) {
            return "MEMBER";
        }

        List<Project> projects = profile.getResume().getProjects();
        if (projects.isEmpty()) {
            return "MEMBER";
        }

        // 최근 프로젝트의 역할 분석
        String recentRole = projects.getFirst().getRole().toLowerCase();

        if (recentRole.contains("리드") || recentRole.contains("lead") || recentRole.contains("pl")) {
            return "LEAD";
        } else if (recentRole.contains("시니어") || recentRole.contains("senior")) {
            return "SENIOR";
        } else if (recentRole.contains("주니어") || recentRole.contains("junior")) {
            return "JUNIOR";
        } else {
            return "MEMBER";
        }
    }

    /**
     * 평균 기술 숙련도 계산
     */
    private double calculateAverageProficiency(Profile profile) {
        if (profile.getProfileSkills() == null || profile.getProfileSkills().isEmpty()) {
            return 0.5;
        }

        Map<String, Double> proficiencyMap = Map.of(
                "BEGINNER", 0.25,
                "INTERMEDIATE", 0.5,
                "ADVANCED", 0.75,
                "EXPERT", 1.0
        );

        return profile.getProfileSkills().stream()
                .mapToDouble(ps -> proficiencyMap.getOrDefault(ps.getProficiency().name(), 0.5))
                .average()
                .orElse(0.5);
    }

    /**
     * 커리어 레벨 결정
     */
    private String determineCareerLevel(
            int experienceYears,
            String projectRole,
            double avgProficiency,
            NcsJobPositionResponse jobPositionResponse
    ) {
        // 경력 기간 기준
        if (experienceYears < 2) {
            return "신입 / 초급";
        } else if (experienceYears < 5) {
            if ("LEAD".equals(projectRole) && avgProficiency >= 0.7) {
                return "중급 실무자";
            }
            return "초급 실무자";
        } else if (experienceYears < 10) {
            if ("LEAD".equals(projectRole) || "SENIOR".equals(projectRole)) {
                return "초급 관리자";
            }
            return "중급 실무자";
        } else {
            return "중급 관리자";
        }
    }

    /**
     * 진행 상황을 SSE로 전송하는 헬퍼 메서드
     */
    private void reportProgress(DiagnosisContext context, int progressPercentage, String message) {
        log.debug("[CompetencyAnalysisProcessor.reportProgress] ENTER - diagnosisId: {}, percentage: {}%, message: {}",
            context.getDiagnosisId(), progressPercentage, message);

        if (context.getProgressCallback() != null) {
            try {
                DiagnosisProgressResponse progressResponse = DiagnosisProgressResponse.builder()
                        .diagnosisId(context.getDiagnosisId())
                        .currentStep(DiagnosisStep.JOB_MATCHING)
                        .progressPercentage(progressPercentage)
                        .status(DiagnosisStatus.IN_PROGRESS)
                        .currentMessage(message)
                        .build();

                context.getProgressCallback().accept(progressResponse);
                log.debug("[CompetencyAnalysisProcessor.reportProgress] EXIT - progress sent successfully");
            } catch (Exception e) {
                log.error("[CompetencyAnalysisProcessor.reportProgress] EXCEPTION - diagnosisId: {}, error: {}",
                    context.getDiagnosisId(), e.getMessage(), e);
            }
        } else {
            log.warn("[CompetencyAnalysisProcessor.reportProgress] No progress callback available - diagnosisId: {}",
                context.getDiagnosisId());
        }
    }

    @Override
    public String getName() {
        return "CompetencyAnalysisProcessor";
    }
}
