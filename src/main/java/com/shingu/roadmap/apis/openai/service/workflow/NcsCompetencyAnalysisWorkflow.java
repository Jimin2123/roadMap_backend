package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NCS 역량 분석 워크플로우
 * KSA 역량 분석 및 NCS 적합도 신뢰도 평가를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NcsCompetencyAnalysisWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final ResumeTextFormatter resumeTextFormatter;

    /**
     * AI 기반 KSA 역량 분석
     * NCS KSA 항목들과 사용자 프로필을 비교하여 각 항목별 보유 수준을 평가합니다.
     *
     * @param ncsCode NCS 코드
     * @param ksaItems NCS KSA 항목 목록
     * @param profile 사용자 프로필
     * @return KSA 항목명과 평가 결과 맵
     */
    @Cacheable(value = OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<Map<String, KsaEvaluationResult>> analyzeKsaCompetency(
            String ncsCode,
            List<String> ksaItems,
            Profile profile
    ) {
        if (ksaItems == null || ksaItems.isEmpty()) {
            return Mono.just(Collections.emptyMap());
        }

        String skillsWithProficiency = profile.getProfileSkills().stream()
                .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
                .collect(Collectors.joining(", "));

        String certificates = profile.getResume() != null
                ? profile.getResume().getCertificates().stream()
                .map(rc -> rc.getCertificate().getJmfldnm())
                .collect(Collectors.joining(", "))
                : "";

        String resumeText = resumeTextFormatter.resumeToText(profile.getResume());

        String systemPrompt = """
                당신은 NCS(국가직무능력표준) 기반 역량 평가 전문가입니다.
                사용자의 이력서와 프로필을 분석하여, NCS KSA 항목별로 보유 수준을 정확하게 평가해야 합니다.

                [평가 기준]
                1. **이력서의 프로젝트 경험**이 가장 중요한 평가 근거입니다
                2. **기술 스택의 숙련도**(BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)를 반영하세요
                3. **자격증**은 보조 지표로 활용하세요
                4. 명시되지 않은 내용은 추론하지 말고, 있는 그대로 평가하세요

                [점수 산정 가이드]
                - 0.9~1.0: 해당 역량에 대한 실무 경험이 풍부하고 전문가 수준
                - 0.7~0.9: 실무 경험이 있으며 숙련된 수준
                - 0.5~0.7: 기본적인 이해와 경험이 있는 수준
                - 0.3~0.5: 이론적 지식은 있으나 실무 경험 부족
                - 0.0~0.3: 거의 경험이 없거나 관련성이 낮음

                [평가 등급]
                - EXCELLENT: 목표 수준을 초과 달성 (gap <= 0)
                - ADEQUATE: 적정 수준 (gap <= 0.1)
                - NEED_IMPROVEMENT: 개선 필요 (gap <= 0.3)
                - INSUFFICIENT: 상당한 역량 강화 필요 (gap > 0.3)
                """;

        String userPrompt = String.format("""
                [사용자 정보]
                - NCS 코드: %s
                - 보유 기술: %s
                - 보유 자격증: %s
                - 이력서:
                %s

                [평가 대상 KSA 항목 목록]
                %s

                [과업]
                위 KSA 항목 각각에 대해 사용자의 보유 수준(userScore, 0.0~1.0)을 평가하고,
                목표 수준(targetScore)은 0.8로 설정한 후,
                각 항목별로 다음 정보를 제공하세요:
                1. userScore: 사용자 보유 수준 (0.0~1.0)
                2. levelAssessment: 평가 등급 (EXCELLENT, ADEQUATE, NEED_IMPROVEMENT, INSUFFICIENT)
                3. gapDescription: 갭에 대한 간단한 설명 (1문장)
                4. recommendation: 역량 강화 방안 (1문장)

                [출력 형식]
                반드시 아래 JSON 형식으로만 응답해주세요. 마크다운 코드 블록이나 설명 없이 순수 JSON만 출력하세요.

                {
                  "ksaScores": {
                    "KSA항목명1": {
                      "userScore": 0.75,
                      "levelAssessment": "ADEQUATE",
                      "gapDescription": "기본적인 역량은 보유하고 있으나 심화 학습이 필요합니다",
                      "recommendation": "관련 프로젝트 경험을 더 쌓으세요"
                    },
                    "KSA항목명2": { ... }
                  }
                }
                """,
                ncsCode,
                skillsWithProficiency,
                certificates,
                resumeText,
                String.join("\n", ksaItems)
        );

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .flatMap(response -> {
                    try {
                        // JSON 파싱
                        String cleanedResponse = response.trim();
                        if (cleanedResponse.startsWith("```")) {
                            cleanedResponse = cleanedResponse
                                    .replaceAll("```json", "")
                                    .replaceAll("```", "")
                                    .trim();
                        }

                        Map<String, Object> parsed = objectMapper.readValue(
                                cleanedResponse,
                                new TypeReference<>() {}
                        );

                        Map<String, Map<String, Object>> ksaScores =
                                (Map<String, Map<String, Object>>) parsed.get("ksaScores");

                        if (ksaScores == null) {
                            log.warn("AI response does not contain ksaScores");
                            return Mono.just(Collections.emptyMap());
                        }

                        // Map<String, KsaEvaluationResult>로 변환
                        Map<String, KsaEvaluationResult> results = new HashMap<>();
                        for (Map.Entry<String, Map<String, Object>> entry : ksaScores.entrySet()) {
                            Map<String, Object> scoreData = entry.getValue();
                            KsaEvaluationResult result = new KsaEvaluationResult(
                                    ((Number) scoreData.get("userScore")).doubleValue(),
                                    (String) scoreData.get("levelAssessment"),
                                    (String) scoreData.get("gapDescription"),
                                    (String) scoreData.get("recommendation")
                            );
                            results.put(entry.getKey(), result);
                        }

                        return Mono.just(results);

                    } catch (Exception e) {
                        log.error("Failed to parse KSA analysis response: {}", response, e);
                        return Mono.just(Collections.emptyMap());
                    }
                });
    }

    /**
     * AI 기반 NCS 적합도 신뢰도 평가
     * 사용자 프로필과 NCS 직무/능력단위 간의 매칭 적합도를 AI로 평가합니다.
     *
     * @param ncsCode NCS 코드
     * @param ncsName NCS 직무명
     * @param compUnitNames 능력단위 목록
     * @param profile 사용자 프로필
     * @return 신뢰도 평가 결과 (0.0~1.0)
     */
    @Cacheable(value = OpenAiCacheConfig.NCS_CODE_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<NcsConfidenceEvaluation> evaluateNcsMatchConfidence(
            String ncsCode,
            String ncsName,
            List<String> compUnitNames,
            Profile profile
    ) {
        String skillsWithProficiency = profile.getProfileSkills().stream()
                .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
                .collect(Collectors.joining(", "));

        String certificates = profile.getResume() != null
                ? profile.getResume().getCertificates().stream()
                .map(rc -> rc.getCertificate().getJmfldnm())
                .collect(Collectors.joining(", "))
                : "";

        String resumeText = resumeTextFormatter.resumeToText(profile.getResume());

        String systemPrompt = """
                당신은 NCS(국가직무능력표준) 기반 직무 적합도를 평가하는 전문 커리어 컨설턴트입니다.
                사용자의 이력서와 프로필을 분석하여, 특정 NCS 직무에 대한 적합도 신뢰도를 정확하게 평가해야 합니다.

                [평가 기준]
                1. **프로젝트 경험의 직접적 관련성** (가중치 40%)
                   - 이력서의 프로젝트가 NCS 직무와 얼마나 직접적으로 연관되는가?
                   - 프로젝트 역할이 NCS 직무 수행과 유사한가?

                2. **기술 스택의 적합성** (가중치 30%)
                   - 보유 기술이 NCS 능력단위 요구사항과 일치하는가?
                   - 기술 숙련도가 직무 수행에 충분한가?

                3. **경력 수준의 적절성** (가중치 20%)
                   - 프로젝트 경험 기간과 역할이 해당 직무 수준과 맞는가?

                4. **자격증 보유** (가중치 10%)
                   - NCS 직무와 관련된 자격증을 보유하고 있는가?

                [신뢰도 점수 가이드]
                - 0.9~1.0: 매우 높은 적합도 - 프로젝트 경험과 기술이 직무 요구사항과 거의 완벽하게 일치
                - 0.8~0.9: 높은 적합도 - 핵심 경험과 기술이 충분하며 직무 수행 가능
                - 0.7~0.8: 적정 적합도 - 기본 경험과 기술은 있으나 일부 보완 필요
                - 0.6~0.7: 낮은 적합도 - 관련 경험은 있으나 직무 수행에 추가 학습 필요
                - 0.0~0.6: 부적합 - 직무와의 연관성이 낮거나 경험 부족
                """;

        String userPrompt = String.format("""
                [평가 대상 NCS 직무]
                - NCS 코드: %s
                - 직무명: %s
                - 능력단위 목록:
                %s

                [사용자 정보]
                - 보유 기술: %s
                - 보유 자격증: %s
                - 이력서:
                %s

                [과업]
                위 [평가 기준]을 바탕으로 사용자가 해당 NCS 직무에 얼마나 적합한지 평가하고,
                다음 정보를 제공하세요:

                1. confidenceScore: 적합도 신뢰도 점수 (0.0~1.0)
                2. matchLevel: 적합 수준 (EXCELLENT, HIGH, ADEQUATE, LOW, POOR)
                3. keyStrengths: 주요 강점 (최대 3개, 문장 배열)
                4. keyWeaknesses: 주요 약점 (최대 3개, 문장 배열)
                5. reasoning: 평가 근거 (2-3문장)

                [출력 형식]
                반드시 아래 JSON 형식으로만 응답해주세요. 마크다운 코드 블록이나 설명 없이 순수 JSON만 출력하세요.

                {
                  "confidenceScore": 0.85,
                  "matchLevel": "HIGH",
                  "keyStrengths": [
                    "백엔드 개발 프로젝트 경험이 풍부함",
                    "Spring Boot 기술 스택이 직무 요구사항과 일치",
                    "팀 리더 역할 수행 경험 보유"
                  ],
                  "keyWeaknesses": [
                    "클라우드 인프라 경험 부족",
                    "대규모 시스템 설계 경험 필요"
                  ],
                  "reasoning": "사용자는 해당 NCS 직무에 필요한 핵심 백엔드 개발 경험과 기술 스택을 충분히 보유하고 있습니다. 다만 일부 능력단위에서 요구하는 클라우드 및 대규모 시스템 경험이 다소 부족하여 추가 학습이 권장됩니다."
                }
                """,
                ncsCode,
                ncsName,
                String.join("\n", compUnitNames.stream().map(name -> "  - " + name).toList()),
                skillsWithProficiency,
                certificates,
                resumeText
        );

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .flatMap(response -> {
                    try {
                        // JSON 파싱
                        String cleanedResponse = response.trim();
                        if (cleanedResponse.startsWith("```")) {
                            cleanedResponse = cleanedResponse
                                    .replaceAll("```json", "")
                                    .replaceAll("```", "")
                                    .trim();
                        }

                        Map<String, Object> parsed = objectMapper.readValue(
                                cleanedResponse,
                                new TypeReference<>() {}
                        );

                        NcsConfidenceEvaluation evaluation = new NcsConfidenceEvaluation(
                                ((Number) parsed.get("confidenceScore")).doubleValue(),
                                (String) parsed.get("matchLevel"),
                                (List<String>) parsed.get("keyStrengths"),
                                (List<String>) parsed.get("keyWeaknesses"),
                                (String) parsed.get("reasoning")
                        );

                        return Mono.just(evaluation);

                    } catch (Exception e) {
                        log.error("Failed to parse NCS confidence evaluation response: {}", response, e);
                        // Fallback: 기본 신뢰도 반환
                        return Mono.just(new NcsConfidenceEvaluation(
                                0.7,
                                "ADEQUATE",
                                Collections.emptyList(),
                                Collections.emptyList(),
                                "AI 평가 실패로 기본 신뢰도를 반환합니다."
                        ));
                    }
                });
    }

    /**
     * NCS 적합도 신뢰도 평가 결과
     */
    public record NcsConfidenceEvaluation(
            double confidenceScore,
            String matchLevel,
            List<String> keyStrengths,
            List<String> keyWeaknesses,
            String reasoning
    ) {}

    /**
     * KSA 평가 결과
     */
    public record KsaEvaluationResult(
            double userScore,
            String levelAssessment,
            String gapDescription,
            String recommendation
    ) {}
}
