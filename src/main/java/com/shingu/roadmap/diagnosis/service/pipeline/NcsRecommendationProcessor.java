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

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.85;
    private static final int MAX_RECOMMENDATION_COUNT = 5;

    @Override
    public DiagnosisContext process(DiagnosisContext context) {
        log.info("[NcsRecommendationProcessor] Starting NCS recommendation for memberId: {}", context.getMemberId());

        try {
            Profile profile = context.getProfile();
            if (profile == null) {
                throw new IllegalArgumentException("Profile is required for NCS recommendation");
            }

            // 1-1. AI를 통한 NCS 코드 추천
            Set<String> recommendedNcsCodes;
            try {
                recommendedNcsCodes = openAiService.recommendNcsCodeUsingAssistant(profile).block();
            } catch (Exception e) {
                log.error("[NcsRecommendationProcessor] AI service error during NCS code recommendation: {}",
                        e.getMessage(), e);
                context.setSuccess(false);
                context.setErrorMessage("AI 서비스 오류로 인해 직무 추천에 실패했습니다. 잠시 후 다시 시도해주세요.");
                return context;
            }

            if (recommendedNcsCodes == null || recommendedNcsCodes.isEmpty()) {
                log.warn("[NcsRecommendationProcessor] No NCS codes recommended by AI (empty result - not an error)");
                context.setSuccess(false);
                context.setErrorMessage("프로필 정보를 기반으로 적합한 직무를 찾지 못했습니다. 프로필을 보완해주세요.");
                return context;
            }

            log.info("AI recommended {} NCS codes: {}", recommendedNcsCodes.size(), recommendedNcsCodes);
            reportProgress(context, 10, "AI가 추천한 NCS 코드를 분석했습니다.");

            // 1-2. NCS API를 통한 유효성 검증 및 등록
            var validOccupations = ncsApiService.filterValidNcsCodes(recommendedNcsCodes);

            if (validOccupations.isEmpty()) {
                log.warn("All recommended NCS codes are invalid");
                context.setSuccess(false);
                context.setErrorMessage("추천된 직무 코드가 유효하지 않습니다.");
                return context;
            }

            reportProgress(context, 20, "NCS 코드 유효성 검증이 완료되었습니다.");

            // 1-3. 능력단위 기반 교차 검증 및 후보 생성
            List<NcsRecommendationCandidate> candidates = new ArrayList<>();
            var validOccupationsList = validOccupations.stream()
                    .limit(MAX_RECOMMENDATION_COUNT)
                    .collect(Collectors.toList());

            int totalOccupations = validOccupationsList.size();
            int processedCount = 0;

            for (var occupation : validOccupationsList) {
                Optional<NcsRecommendationCandidate> candidate =
                    buildCandidateWithCompUnitValidation(occupation.getDutyCd(), profile, context);
                candidate.ifPresent(candidates::add);

                processedCount++;
                // 20% ~ 28% 범위에서 진행률 업데이트
                int progress = 20 + (8 * processedCount / totalOccupations);
                reportProgress(context, progress,
                    String.format("직무 후보 검증 중... (%d/%d)", processedCount, totalOccupations));
            }

            candidates = candidates.stream()
                    .sorted(Comparator.comparing(NcsRecommendationCandidate::confidenceScore).reversed())
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                log.warn("Failed to build any valid candidates");
                context.setSuccess(false);
                context.setErrorMessage("직무 후보 생성에 실패했습니다.");
                return context;
            }

            reportProgress(context, 28, "신뢰도 평가를 시작합니다.");

            // 1-4. 신뢰도 기반 자동 선택 또는 사용자 선택 요청
            double overallConfidence = calculateOverallConfidence(candidates);
            boolean requiresUserSelection = overallConfidence < HIGH_CONFIDENCE_THRESHOLD;
            String selectedNcsCode = !requiresUserSelection ? candidates.get(0).ncsCode() : null;

            NcsAnalysisResponse ncsAnalysisResponse = NcsAnalysisResponse.builder()
                    .candidates(candidates)
                    .overallConfidence(overallConfidence)
                    .requiresUserSelection(requiresUserSelection)
                    .selectedNcsCode(selectedNcsCode)
                    .build();

            context.setNcsAnalysisResponse(ncsAnalysisResponse);
            context.setSuccess(true);

            reportProgress(context, 33, "NCS 직무 추천이 완료되었습니다.");

            log.info("[NcsRecommendationProcessor] Completed. Confidence: {}, Requires user selection: {}",
                    overallConfidence, requiresUserSelection);

            return context;

        } catch (Exception e) {
            log.error("[NcsRecommendationProcessor] Failed to process: {}", e.getMessage(), e);
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
        try {
            // NCS 직무 정보 조회
            NcsOccupationResponse occupationResponse = ncsApiService.fetchAndRegisterNcsOccupation(ncsCode)
                    ? getNcsOccupationInfo(ncsCode)
                    : null;

            if (occupationResponse == null || occupationResponse.data() == null || occupationResponse.data().isEmpty()) {
                log.warn("Failed to fetch occupation info for NCS code: {}", ncsCode);
                return Optional.empty();
            }

            var occupationItem = occupationResponse.data().get(0);
            String ncsName = occupationItem.dutyNm();

            // 능력단위 정보 조회 (교차 검증용)
            NcsCompUnitResponse compUnitResponse = ncsApiService.getNcsCompUnit(ncsCode);
            List<String> compUnitNames = extractCompUnitNames(compUnitResponse);

            // 규칙 기반 신뢰도 계산
            double ruleBasedConfidence = calculateRuleBasedConfidence(profile, compUnitNames);

            // AI 기반 신뢰도 평가
            OpenAiService.NcsConfidenceEvaluation aiEvaluation = openAiService
                    .evaluateNcsMatchConfidence(ncsCode, ncsName, compUnitNames, profile)
                    .onErrorResume(e -> {
                        log.warn("AI confidence evaluation failed for {}: {}", ncsCode, e.getMessage());
                        return reactor.core.publisher.Mono.just(new OpenAiService.NcsConfidenceEvaluation(
                                ruleBasedConfidence,
                                "ADEQUATE",
                                Collections.emptyList(),
                                Collections.emptyList(),
                                "AI 평가 실패로 규칙 기반 신뢰도를 사용합니다."
                        ));
                    })
                    .block();

            // 최종 신뢰도: AI 평가(70%) + 규칙 기반(30%) 가중 평균
            double finalConfidence = (aiEvaluation.confidenceScore() * 0.7) + (ruleBasedConfidence * 0.3);

            log.info("Confidence for {}: AI={}, Rule={}, Final={}",
                    ncsCode, aiEvaluation.confidenceScore(), ruleBasedConfidence, finalConfidence);

            // AI 평가 결과를 활용한 추천 근거 생성
            List<Evidence> evidenceList = generateEvidenceFromAiEvaluation(profile, aiEvaluation);
            String reason = generateReasonFromAiEvaluation(ncsName, aiEvaluation);

            return Optional.of(NcsRecommendationCandidate.builder()
                    .ncsCode(ncsCode)
                    .ncsName(ncsName)
                    .confidenceScore(finalConfidence)
                    .reason(reason)
                    .evidenceList(evidenceList)
                    .build());

        } catch (Exception e) {
            log.error("Failed to build candidate for NCS code {}: {}", ncsCode, e.getMessage());
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
        String resumeText = openAiService.resumeToText(profile.getResume()).toLowerCase();
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
            OpenAiService.NcsConfidenceEvaluation aiEvaluation
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
            OpenAiService.NcsConfidenceEvaluation aiEvaluation
    ) {
        StringBuilder reason = new StringBuilder();

        reason.append(String.format("%s 직무는 귀하의 프로필과 %s 수준의 적합도를 보입니다. ",
                ncsName,
                getMatchLevelDescription(aiEvaluation.matchLevel())
        ));

        reason.append(aiEvaluation.reasoning());

        return reason.toString();
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
        if (context.getProgressCallback() != null) {
            DiagnosisProgressResponse progressResponse = DiagnosisProgressResponse.builder()
                    .diagnosisId(context.getDiagnosisId())
                    .currentStep(DiagnosisStep.NCS_CODE_SUGGESTION)
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
        return "NcsRecommendationProcessor";
    }
}
