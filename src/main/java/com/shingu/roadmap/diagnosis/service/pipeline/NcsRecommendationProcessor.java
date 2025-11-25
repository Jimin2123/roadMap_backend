package com.shingu.roadmap.diagnosis.service.pipeline;

import com.shingu.roadmap.apis.ncs.dto.response.NcsCompUnitResponse;
import com.shingu.roadmap.apis.ncs.dto.response.NcsOccupationResponse;
import com.shingu.roadmap.apis.ncs.service.NcsApiService;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStatus;
import com.shingu.roadmap.diagnosis.domain.DiagnosisStep;
import com.shingu.roadmap.diagnosis.dto.common.Evidence;
import com.shingu.roadmap.diagnosis.dto.common.EvidenceSourceType;
import com.shingu.roadmap.diagnosis.dto.common.NcsRecommendationCandidate;
import com.shingu.roadmap.diagnosis.dto.response.DiagnosisProgressResponse;
import com.shingu.roadmap.diagnosis.dto.response.NcsAnalysisResponse;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 1단계: 사용자 맞춤 NCS 직무 추천 및 검증 프로세서
 *
 * 처리 흐름:
 * 1-1. AI가 사용자 프로필을 분석하여 적합한 NCS 직무 후보를 추천
 * 1-2. 추천된 NCS 코드의 유효성을 NCS API로 검증
 * 1-3. 능력단위(Competency Unit) 기반 교차 검증으로 신뢰도 강화
 * 1-4. 신뢰도가 낮으면 사용자 선택 요청, 높으면 자동 선택
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NcsRecommendationProcessor implements DiagnosisProcessor {

    private final OpenAiService openAiService;
    private final NcsApiService ncsApiService;
    private final com.shingu.roadmap.apis.openai.util.ResumeTextFormatter resumeTextFormatter;

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.85;
    private static final int MAX_RECOMMENDATION_COUNT = 5;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[NcsRecommendationProcessor.process] ENTER - memberId: {}, diagnosisId: {}",
            context.getMemberId(), context.getDiagnosisId());
        long totalStartTime = System.currentTimeMillis();

        try {
            Profile profile = context.getProfile();
            if (profile == null) {
                long duration = System.currentTimeMillis() - totalStartTime;
                log.error("[NcsRecommendationProcessor.process] Profile is null - aborting, duration: {}ms", duration);
                context.setSuccess(false);
                context.setErrorMessage("Required analysis results missing: Profile is null");
                log.info("[NcsRecommendationProcessor.process] EXIT (PROFILE MISSING) - duration: {}ms", duration);
                return context;
            }
            log.debug("[NcsRecommendationProcessor.process] Profile loaded - skillCount: {}, projectCount: {}",
                profile.getProfileSkills() != null ? profile.getProfileSkills().size() : 0,
                profile.getResume() != null && profile.getResume().getProjects() != null ?
                    profile.getResume().getProjects().size() : 0);

            // 1-1. AI를 통한 NCS 코드 추천
            log.debug("[NcsRecommendationProcessor.process] Starting AI recommendation");
            Set<String> recommendedNcsCodes;
            long aiStartTime = System.currentTimeMillis();
            try {
                recommendedNcsCodes = openAiService.recommendNcsCodeUsingAssistant(profile).block();
                long aiDuration = System.currentTimeMillis() - aiStartTime;
                log.info("[NcsRecommendationProcessor.process] AI recommendation completed in {}ms - recommendedCount: {}",
                    aiDuration, recommendedNcsCodes != null ? recommendedNcsCodes.size() : 0);
            } catch (Exception e) {
                long aiDuration = System.currentTimeMillis() - aiStartTime;
                log.error("[NcsRecommendationProcessor.process] AI service error after {}ms - error: {}",
                        aiDuration, e.getMessage(), e);
                context.setSuccess(false);
                context.setErrorMessage("AI 서비스 오류로 인해 직무 추천에 실패했습니다. 잠시 후 다시 시도해주세요.");
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.info("[NcsRecommendationProcessor.process] EXIT (AI FAILURE) - totalDuration: {}ms", totalDuration);
                return context;
            }

            if (recommendedNcsCodes == null || recommendedNcsCodes.isEmpty()) {
                log.warn("[NcsRecommendationProcessor.process] No NCS codes recommended by AI (empty result)");
                context.setSuccess(false);
                context.setErrorMessage("프로필 정보를 기반으로 적합한 직무를 찾지 못했습니다. 프로필을 보완해주세요.");
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.info("[NcsRecommendationProcessor.process] EXIT (EMPTY RESULT) - totalDuration: {}ms", totalDuration);
                return context;
            }

            log.info("[NcsRecommendationProcessor.process] AI recommended {} NCS codes: {}",
                recommendedNcsCodes.size(), recommendedNcsCodes);
            reportProgress(context, 10, "AI가 추천한 NCS 코드를 분석했습니다.");

            // 1-2. NCS API를 통한 유효성 검증 및 등록
            log.debug("[NcsRecommendationProcessor.process] Starting NCS API validation");
            long validationStartTime = System.currentTimeMillis();
            var validOccupations = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);
            long validationDuration = System.currentTimeMillis() - validationStartTime;
            log.info("[NcsRecommendationProcessor.process] NCS API validation completed in {}ms - validCount: {}/{}",
                validationDuration, validOccupations.size(), recommendedNcsCodes.size());

            if (validOccupations.isEmpty()) {
                log.warn("[NcsRecommendationProcessor.process] All recommended NCS codes are invalid");
                context.setSuccess(false);
                context.setErrorMessage("추천된 직무 코드가 유효하지 않습니다.");
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.info("[NcsRecommendationProcessor.process] EXIT (INVALID CODES) - totalDuration: {}ms", totalDuration);
                return context;
            }

            reportProgress(context, 20, "NCS 코드 유효성 검증이 완료되었습니다.");

            // 1-3. 능력단위 기반 교차 검증 및 후보 생성
            log.debug("[NcsRecommendationProcessor.process] Starting candidate building with competency unit validation");
            List<NcsRecommendationCandidate> candidates = new ArrayList<>();
            var validOccupationsList = validOccupations.stream()
                    .limit(MAX_RECOMMENDATION_COUNT)
                    .toList();

            int totalOccupations = validOccupationsList.size();
            int processedCount = 0;
            log.info("[NcsRecommendationProcessor.process] Processing {} valid occupations (limited to {})",
                totalOccupations, MAX_RECOMMENDATION_COUNT);

            long candidateBuildingStartTime = System.currentTimeMillis();
            for (var occupation : validOccupationsList) {
                log.debug("[NcsRecommendationProcessor.process] Building candidate {}/{} - ncsCode: {}",
                    processedCount + 1, totalOccupations, occupation.getDutyCd());
                long candidateStartTime = System.currentTimeMillis();
                Optional<NcsRecommendationCandidate> candidate =
                    buildCandidateWithCompUnitValidation(occupation.getDutyCd(), profile, context);
                long candidateDuration = System.currentTimeMillis() - candidateStartTime;

                if (candidate.isPresent()) {
                    candidates.add(candidate.get());
                    log.debug("[NcsRecommendationProcessor.process] Candidate built successfully in {}ms - ncsCode: {}, confidence: {}",
                        candidateDuration, candidate.get().ncsCode(), candidate.get().confidenceScore());
                } else {
                    log.warn("[NcsRecommendationProcessor.process] Failed to build candidate in {}ms - ncsCode: {}",
                        candidateDuration, occupation.getDutyCd());
                }

                processedCount++;
                // 20% ~ 28% 범위에서 진행률 업데이트
                int progress = 20 + (8 * processedCount / totalOccupations);
                reportProgress(context, progress,
                    String.format("직무 후보 검증 중... (%d/%d)", processedCount, totalOccupations));
            }
            long candidateBuildingDuration = System.currentTimeMillis() - candidateBuildingStartTime;
            log.info("[NcsRecommendationProcessor.process] Candidate building completed in {}ms - candidatesBuilt: {}/{}",
                candidateBuildingDuration, candidates.size(), totalOccupations);

            // 신뢰도 순 정렬
            log.debug("[NcsRecommendationProcessor.process] Sorting candidates by confidence score");
            candidates = candidates.stream()
                    .sorted(Comparator.comparing(NcsRecommendationCandidate::confidenceScore).reversed())
                    .collect(Collectors.toList());
            log.debug("[NcsRecommendationProcessor.process] Top candidate: ncsCode={}, confidence={}",
                candidates.isEmpty() ? "none" : candidates.getFirst().ncsCode(),
                candidates.isEmpty() ? 0.0 : candidates.getFirst().confidenceScore());

            if (candidates.isEmpty()) {
                log.warn("[NcsRecommendationProcessor.process] Failed to build any valid candidates");
                context.setSuccess(false);
                context.setErrorMessage("직무 후보 생성에 실패했습니다.");
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.info("[NcsRecommendationProcessor.process] EXIT (NO CANDIDATES) - totalDuration: {}ms", totalDuration);
                return context;
            }

            reportProgress(context, 28, "신뢰도 평가를 시작합니다.");

            // 1-4. 신뢰도 기반 자동 선택 또는 사용자 선택 요청
            log.debug("[NcsRecommendationProcessor.process] Calculating overall confidence");
            double overallConfidence = calculateOverallConfidence(candidates);
            boolean requiresUserSelection = overallConfidence < HIGH_CONFIDENCE_THRESHOLD;
            String selectedNcsCode = !requiresUserSelection ? candidates.getFirst().ncsCode() : null;
            log.info("[NcsRecommendationProcessor.process] Overall confidence: {} (threshold: {}), requiresUserSelection: {}, selectedNcsCode: {}",
                overallConfidence, HIGH_CONFIDENCE_THRESHOLD, requiresUserSelection, selectedNcsCode != null ? selectedNcsCode : "none");

            NcsAnalysisResponse ncsAnalysisResponse = NcsAnalysisResponse.builder()
                    .candidates(candidates)
                    .overallConfidence(overallConfidence)
                    .requiresUserSelection(requiresUserSelection)
                    .selectedNcsCode(selectedNcsCode)
                    .build();

            context.setNcsAnalysisResponse(ncsAnalysisResponse);
            context.setSuccess(true);

            reportProgress(context, 33, "NCS 직무 추천이 완료되었습니다.");

            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.info("[NcsRecommendationProcessor.process] EXIT (SUCCESS) - totalDuration: {}ms, candidatesCount: {}, overallConfidence: {}, requiresUserSelection: {}",
                    totalDuration, candidates.size(), overallConfidence, requiresUserSelection);

            return context;

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            log.error("[NcsRecommendationProcessor.process] EXCEPTION - memberId: {}, totalDuration: {}ms, error: {}",
                context.getMemberId(), totalDuration, e.getMessage(), e);
            context.setSuccess(false);
            context.setErrorMessage("NCS 직무 추천 중 오류가 발생했습니다: " + e.getMessage());
            return context;
        }
    }

    /**
     * 능력단위 API를 통한 교차 검증으로 NCS 후보 생성 (AI 기반 신뢰도 평가 포함)
     */
    private Optional<NcsRecommendationCandidate> buildCandidateWithCompUnitValidation(
            String ncsCode, Profile profile, DiagnosisContext context) {
        log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] ENTER - ncsCode: {}", ncsCode);
        long startTime = System.currentTimeMillis();

        try {
            // NCS 직무 정보 조회
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Fetching and registering NCS occupation");
            long fetchStartTime = System.currentTimeMillis();
            boolean registered = ncsApiService.fetchAndRegisterNcsOccupation(ncsCode);
            long fetchDuration = System.currentTimeMillis() - fetchStartTime;
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Fetch and register completed in {}ms - registered: {}",
                fetchDuration, registered);

            NcsOccupationResponse occupationResponse = registered ? getNcsOccupationInfo(ncsCode) : null;

            if (occupationResponse == null || occupationResponse.data() == null || occupationResponse.data().isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Failed to fetch occupation info - ncsCode: {}, duration: {}ms",
                    ncsCode, duration);
                return Optional.empty();
            }

            var occupationItem = occupationResponse.data().getFirst();
            String ncsName = occupationItem.dutyNm();
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Occupation info retrieved - ncsName: {}", ncsName);

            // 능력단위 정보 조회 (교차 검증용)
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Fetching competency units");
            long compUnitStartTime = System.currentTimeMillis();
            NcsCompUnitResponse compUnitResponse = ncsApiService.getNcsCompUnit(ncsCode);
            List<String> compUnitNames = extractCompUnitNames(compUnitResponse);
            long compUnitDuration = System.currentTimeMillis() - compUnitStartTime;
            log.info("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Competency units fetched in {}ms - count: {}",
                compUnitDuration, compUnitNames.size());

            // 규칙 기반 신뢰도 계산
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Calculating rule-based confidence");
            long ruleStartTime = System.currentTimeMillis();
            double ruleBasedConfidence = calculateRuleBasedConfidence(profile, compUnitNames);
            long ruleDuration = System.currentTimeMillis() - ruleStartTime;
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Rule-based confidence calculated in {}ms - score: {}",
                ruleDuration, ruleBasedConfidence);

            // AI 기반 신뢰도 평가
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Starting AI confidence evaluation");
            long aiEvalStartTime = System.currentTimeMillis();
            com.shingu.roadmap.apis.openai.service.workflow.NcsCompetencyAnalysisWorkflow.NcsConfidenceEvaluation aiEvaluation = openAiService
                    .evaluateNcsMatchConfidence(ncsCode, ncsName, compUnitNames, profile)
                    .onErrorResume(e -> {
                        log.warn("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] AI evaluation failed for {} - error: {}, using fallback",
                            ncsCode, e.getMessage());
                        return reactor.core.publisher.Mono.just(new com.shingu.roadmap.apis.openai.service.workflow.NcsCompetencyAnalysisWorkflow.NcsConfidenceEvaluation(
                                ruleBasedConfidence,
                                "ADEQUATE",
                                Collections.emptyList(),
                                Collections.emptyList(),
                                "AI 평가 실패로 규칙 기반 신뢰도를 사용합니다."
                        ));
                    })
                    .block();
            long aiEvalDuration = System.currentTimeMillis() - aiEvalStartTime;
            log.info("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] AI evaluation completed in {}ms - aiScore: {}, matchLevel: {}",
                aiEvalDuration, Objects.requireNonNull(aiEvaluation).confidenceScore(), aiEvaluation.matchLevel());

            // 최종 신뢰도: AI 평가(60%) + 규칙 기반(40%) 가중 평균
            // Phase 2 P3: 하이브리드 접근으로 규칙 기반 비중 증가 (정확도 향상)
            double finalConfidence = (aiEvaluation.confidenceScore() * 0.6) + (ruleBasedConfidence * 0.4);
            log.info("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Final confidence calculated - ncsCode: {}, AI: {}, Rule: {}, Final: {}",
                    ncsCode, aiEvaluation.confidenceScore(), ruleBasedConfidence, finalConfidence);

            // AI 평가 결과를 활용한 추천 근거 생성
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Generating evidence and reason");
            List<Evidence> evidenceList = generateEvidenceFromAiEvaluation(profile, aiEvaluation);
            String reason = generateReasonFromAiEvaluation(ncsName, aiEvaluation);
            log.debug("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] Evidence count: {}", evidenceList.size());

            NcsRecommendationCandidate candidate = NcsRecommendationCandidate.builder()
                    .ncsCode(ncsCode)
                    .ncsName(ncsName)
                    .confidenceScore(finalConfidence)
                    .reason(reason)
                    .evidenceList(evidenceList)
                    .build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] EXIT (SUCCESS) - ncsCode: {}, duration: {}ms, finalConfidence: {}",
                ncsCode, duration, finalConfidence);
            return Optional.of(candidate);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[NcsRecommendationProcessor.buildCandidateWithCompUnitValidation] EXCEPTION - ncsCode: {}, duration: {}ms, error: {}",
                ncsCode, duration, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * NCS 직무 정보 조회
     */
    private NcsOccupationResponse getNcsOccupationInfo(String ncsCode) {
        try {
            return ncsApiService.getOccupation(ncsCode);
        } catch (Exception e) {
            log.error("Failed to get NCS occupation info for code {}: {}", ncsCode, e.getMessage());
            return null;
        }
    }

    /**
     * 능력단위 응답에서 능력단위명 추출
     */
    private List<String> extractCompUnitNames(NcsCompUnitResponse response) {
        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }
        return response.data().stream()
                .map(NcsCompUnitResponse.NcsCompUnitItem::compUnitName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 규칙 기반 신뢰도 계산
     */
    private double calculateRuleBasedConfidence(Profile profile, List<String> compUnitNames) {
        // 기본 신뢰도
        double baseConfidence = 0.7;

        // 사용자 스킬 매칭도
        long matchingSkills = profile.getProfileSkills().stream()
                .filter(ps -> compUnitNames.stream()
                        .anyMatch(compUnit -> compUnit.contains(ps.getSkill().getName())))
                .count();

        double skillBonus = Math.min(0.2, matchingSkills * 0.05);

        // 프로젝트 경험 매칭도
        String resumeText = resumeTextFormatter.resumeToText(profile.getResume()).toLowerCase();
        long matchingCompUnits = compUnitNames.stream()
                .filter(compUnit -> resumeText.contains(compUnit.toLowerCase()))
                .count();

        double experienceBonus = Math.min(0.1, matchingCompUnits * 0.03);

        return Math.min(1.0, baseConfidence + skillBonus + experienceBonus);
    }

    /**
     * AI 평가 결과를 기반으로 추천 근거 생성
     */
    private List<Evidence> generateEvidenceFromAiEvaluation(
            Profile profile,
            com.shingu.roadmap.apis.openai.service.workflow.NcsCompetencyAnalysisWorkflow.NcsConfidenceEvaluation aiEvaluation
    ) {
        List<Evidence> evidences = new ArrayList<>();

        // AI가 식별한 강점을 근거로 추가
        aiEvaluation.keyStrengths().stream()
                .limit(3)
                .forEach(strength -> evidences.add(Evidence.builder()
                        .sourceType(EvidenceSourceType.AI_ANALYSIS)
                        .sourceDetail("AI 분석 - 강점")
                        .content(strength)
                        .reasoning("직무 적합도 분석 결과")
                        .build()));

        // 기본 스킬 근거 추가 (AI 강점 외 추가 근거)
        profile.getProfileSkills().stream()
                .limit(2)
                .forEach(ps -> evidences.add(Evidence.builder()
                        .sourceType(EvidenceSourceType.SKILL)
                        .sourceDetail("보유 기술")
                        .content(ps.getSkill().getName() + " (" + ps.getProficiency() + ")")
                        .reasoning("핵심 기술 보유")
                        .build()));

        // 프로젝트 경험 근거 추가
        if (profile.getResume() != null && profile.getResume().getProjects() != null) {
            profile.getResume().getProjects().stream()
                    .limit(1)
                    .forEach(project -> evidences.add(Evidence.builder()
                            .sourceType(EvidenceSourceType.PROJECT)
                            .sourceDetail("프로젝트 경험")
                            .content(project.getName() + " - " + project.getRole())
                            .reasoning("실무 경험 보유")
                            .build()));
        }

        return evidences;
    }

    /**
     * AI 평가 결과를 기반으로 추천 이유 생성
     */
    private String generateReasonFromAiEvaluation(
            String ncsName,
            com.shingu.roadmap.apis.openai.service.workflow.NcsCompetencyAnalysisWorkflow.NcsConfidenceEvaluation aiEvaluation
    ) {

      return String.format("%s 직무는 귀하의 프로필과 %s 수준의 적합도를 보입니다. ",
              ncsName,
              getMatchLevelDescription(aiEvaluation.matchLevel())
      ) +
              aiEvaluation.reasoning();
    }

    /**
     * 적합 수준 한글 설명 반환
     */
    private String getMatchLevelDescription(String matchLevel) {
        return switch (matchLevel) {
            case "EXCELLENT" -> "매우 높은";
            case "HIGH" -> "높은";
            case "ADEQUATE" -> "적정";
            case "LOW" -> "낮은";
            case "POOR" -> "부족한";
            default -> "적정";
        };
    }

    /**
     * 추천 근거 목록 생성
     */
    private List<Evidence> generateEvidenceList(Profile profile, List<String> compUnitNames) {
        List<Evidence> evidences = new ArrayList<>();

        // 스킬 기반 근거
        profile.getProfileSkills().stream()
                .limit(3)
                .forEach(ps -> evidences.add(Evidence.builder()
                        .sourceType(EvidenceSourceType.SKILL)
                        .sourceDetail("보유 기술")
                        .content(ps.getSkill().getName() + " (" + ps.getProficiency() + ")")
                        .reasoning("핵심 기술 보유")
                        .build()));

        // 프로젝트 기반 근거
        if (profile.getResume() != null && profile.getResume().getProjects() != null) {
            profile.getResume().getProjects().stream()
                    .limit(2)
                    .forEach(project -> evidences.add(Evidence.builder()
                            .sourceType(EvidenceSourceType.PROJECT)
                            .sourceDetail("프로젝트 경험")
                            .content(project.getName() + " - " + project.getRole())
                            .reasoning("실무 경험 보유")
                            .build()));
        }

        return evidences;
    }

    /**
     * 추천 이유 생성
     */
    private String generateRecommendationReason(String dutyName, List<String> compUnitNames, Profile profile) {
        return String.format("%s 직무는 귀하의 기술 스택 및 프로젝트 경험과 높은 연관성을 보입니다. " +
                        "특히 %s 등의 능력단위에서 강점을 보유하고 있습니다.",
                dutyName,
                compUnitNames.stream().limit(2).collect(Collectors.joining(", ")));
    }

    /**
     * 전체 신뢰도 계산
     */
    private double calculateOverallConfidence(List<NcsRecommendationCandidate> candidates) {
        return candidates.stream()
                .mapToDouble(NcsRecommendationCandidate::confidenceScore)
                .average()
                .orElse(0.0);
    }

    /**
     * 진행 상황을 SSE로 전송하는 헬퍼 메서드
     */
    private void reportProgress(DiagnosisContext context, int progressPercentage, String message) {
        log.debug("[NcsRecommendationProcessor.reportProgress] ENTER - diagnosisId: {}, percentage: {}%, message: {}",
            context.getDiagnosisId(), progressPercentage, message);

        if (context.getProgressCallback() != null) {
            try {
                DiagnosisProgressResponse progressResponse = DiagnosisProgressResponse.builder()
                        .diagnosisId(context.getDiagnosisId())
                        .currentStep(DiagnosisStep.NCS_CODE_SUGGESTION)
                        .progressPercentage(progressPercentage)
                        .status(DiagnosisStatus.IN_PROGRESS)
                        .currentMessage(message)
                        .build();

                context.getProgressCallback().accept(progressResponse);
                log.debug("[NcsRecommendationProcessor.reportProgress] EXIT - progress sent successfully");
            } catch (Exception e) {
                log.error("[NcsRecommendationProcessor.reportProgress] EXCEPTION - diagnosisId: {}, error: {}",
                    context.getDiagnosisId(), e.getMessage(), e);
            }
        } else {
            log.warn("[NcsRecommendationProcessor.reportProgress] No progress callback available - diagnosisId: {}",
                context.getDiagnosisId());
        }
    }

    @Override
    public String getName() {
        return "NcsRecommendationProcessor";
    }
}
