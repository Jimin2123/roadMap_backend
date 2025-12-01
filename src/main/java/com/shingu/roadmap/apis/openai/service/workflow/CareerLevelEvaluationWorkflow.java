package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.util.JsonResponseParser;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.diagnosis.domain.CareerLevel;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 기반 경력 레벨 평가 워크플로우 (Phase 3 A1)
 *
 * 사용자의 이력서를 종합 분석하여 커리어 레벨을 평가합니다.
 * 단순 연수가 아닌 다차원 평가를 수행합니다:
 * - 경력 연수
 * - 프로젝트 규모 및 역할
 * - 기술 깊이
 * - 리더십 경험
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareerLevelEvaluationWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final JsonResponseParser jsonResponseParser;
    private final ResumeTextFormatter resumeTextFormatter;

    /**
     * AI 기반 경력 레벨 평가 결과
     *
     * @param careerLevel 평가된 경력 레벨
     * @param confidence 신뢰도 (0.0 ~ 1.0)
     * @param reasoning 평가 근거
     * @param strengths 강점 목록
     * @param developmentAreas 개선 영역
     */
    public record CareerLevelEvaluation(
            CareerLevel careerLevel,
            double confidence,
            String reasoning,
            List<String> strengths,
            List<String> developmentAreas
    ) {}

    /**
     * 프로필을 기반으로 경력 레벨을 AI로 평가합니다.
     *
     * @param profile 사용자 프로필
     * @return 경력 레벨 평가 결과
     */
    @Cacheable(value = OpenAiCacheConfig.CAREER_LEVEL_EVALUATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<CareerLevelEvaluation> evaluateCareerLevel(Profile profile) {
        log.info("[CareerLevelEvaluationWorkflow] Evaluating career level for profile: {}", profile.getId());

        // 1. 사용자 컨텍스트 구성
        String userContext = buildUserContext(profile);

        // 2. AI 프롬프트 생성
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(userContext);

        // 3. OpenAI API 호출
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .flatMap(response -> {
                    try {
                        // 4. JSON 응답 파싱
                        @SuppressWarnings("unchecked")
                        Map<String, Object> evaluation = (Map<String, Object>) jsonResponseParser.parseType(
                                response,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {},
                                Map.of()
                        );

                        // 5. 평가 결과 구성
                        CareerLevel careerLevel = parseCareerLevel((String) evaluation.get("careerLevel"));
                        double confidence = ((Number) evaluation.getOrDefault("confidence", 0.7)).doubleValue();
                        String reasoning = (String) evaluation.getOrDefault("reasoning", "");

                        @SuppressWarnings("unchecked")
                        List<String> strengths = (List<String>) evaluation.getOrDefault("strengths", List.of());

                        @SuppressWarnings("unchecked")
                        List<String> developmentAreas = (List<String>) evaluation.getOrDefault("developmentAreas", List.of());

                        CareerLevelEvaluation result = new CareerLevelEvaluation(
                                careerLevel,
                                confidence,
                                reasoning,
                                strengths,
                                developmentAreas
                        );

                        log.info("[CareerLevelEvaluationWorkflow] Evaluation completed - level: {}, confidence: {}",
                                careerLevel, confidence);
                        return Mono.just(result);

                    } catch (Exception e) {
                        log.error("[CareerLevelEvaluationWorkflow] Failed to parse AI response: {}", e.getMessage());
                        // Fallback: 경력 연수 기반 추정
                        return Mono.just(fallbackEvaluation(profile));
                    }
                })
                .onErrorResume(e -> {
                    log.error("[CareerLevelEvaluationWorkflow] AI evaluation failed: {}", e.getMessage(), e);
                    return Mono.just(fallbackEvaluation(profile));
                });
    }

    /**
     * 사용자 컨텍스트 구성
     */
    private String buildUserContext(Profile profile) {
        StringBuilder context = new StringBuilder();

        // 기본 정보
        context.append("## 기본 정보\n");
        context.append("- 학력: ").append(profile.getEducationLevel() != null ? profile.getEducationLevel() : "미기재").append("\n");

        // 보유 스킬
        if (profile.getProfileSkills() != null && !profile.getProfileSkills().isEmpty()) {
            context.append("\n## 보유 스킬\n");
            String skills = profile.getProfileSkills().stream()
                    .map(ps -> String.format("%s (%s)", ps.getSkill().getName(), ps.getProficiency()))
                    .collect(Collectors.joining(", "));
            context.append(skills).append("\n");
        }

        // 이력서 전체 (경력, 프로젝트, 자격증 등)
        if (profile.getResume() != null) {
            context.append("\n## 이력서\n");
            context.append(resumeTextFormatter.resumeToText(profile.getResume()));
        }

        return context.toString();
    }

    /**
     * System Prompt 구성
     */
    private String buildSystemPrompt() {
        return """
                당신은 경력 개발 전문가이자 시니어 개발자입니다.
                사용자의 이력서와 프로필을 분석하여 커리어 레벨을 정확하게 평가해야 합니다.

                [평가 기준]
                단순 경력 연수가 아닌 다차원 평가를 수행하세요:
                1. **경력 연수** - 기본 지표
                2. **프로젝트 규모** - 팀 크기, 사용자 수, 복잡도
                3. **프로젝트 역할** - 팀원, 팀장, PM, 아키텍트
                4. **기술 깊이** - 단순 사용 vs 전문가 수준 활용
                5. **리더십** - 멘토링, 발표, 오픈소스 기여, 기술 블로그

                [레벨 정의]
                - **JUNIOR**: 0-3년, 지도 하에 작업, 기본 기술 활용
                - **MID**: 3-7년, 독립적 업무, 중급 기술, 일부 멘토링
                - **SENIOR**: 7-12년, 전문가 수준, 아키텍처 설계, 팀 리딩
                - **LEAD**: 12년 이상, 기술 의사결정, 전사 아키텍처, 다수 팀 리딩

                [신뢰도 산정]
                - 1.0: 명확한 증거가 풍부함
                - 0.7-0.9: 충분한 증거
                - 0.5-0.7: 일부 증거
                - 0.3-0.5: 추정 기반

                [주의사항]
                - 이력서에 명시된 내용만 평가하세요
                - 추론이나 가정은 최소화하세요
                - 실제 프로젝트 경험과 성과를 중시하세요
                """;
    }

    /**
     * User Prompt 구성
     */
    private String buildUserPrompt(String userContext) {
        return String.format("""
                [사용자 정보]
                %s

                [과업]
                위 정보를 바탕으로 사용자의 커리어 레벨을 평가하고, 아래 JSON 형식으로 응답해주세요.
                마크다운 코드 블록이나 설명 없이 순수 JSON만 출력하세요.

                {
                  "careerLevel": "JUNIOR|MID|SENIOR|LEAD",
                  "confidence": 0.85,
                  "reasoning": "평가 근거를 2-3문장으로 설명",
                  "strengths": [
                    "강점 1",
                    "강점 2",
                    "강점 3"
                  ],
                  "developmentAreas": [
                    "개선 영역 1",
                    "개선 영역 2"
                  ]
                }
                """, userContext);
    }

    /**
     * 문자열을 CareerLevel enum으로 파싱
     */
    private CareerLevel parseCareerLevel(String levelStr) {
        if (levelStr == null || levelStr.isBlank()) {
            return CareerLevel.JUNIOR;
        }

        try {
            return CareerLevel.valueOf(levelStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("[CareerLevelEvaluationWorkflow] Unknown career level: {}, defaulting to JUNIOR", levelStr);
            return CareerLevel.JUNIOR;
        }
    }

    /**
     * AI 실패 시 fallback 평가 (경력 연수 기반)
     */
    private CareerLevelEvaluation fallbackEvaluation(Profile profile) {
        // 경력 연수 계산
        double careerYears = 0.0;
        if (profile.getResume() != null && profile.getResume().getCareers() != null) {
            careerYears = profile.getResume().getCareers().stream()
                    .mapToDouble(career -> {
                        if (career.getPeriod() == null) return 0.0;
                        var period = career.getPeriod();
                        if (period.getStartDate() == null) return 0.0;
                        var endDate = period.getEndDate() != null ?
                                period.getEndDate() : java.time.LocalDate.now();
                        return java.time.temporal.ChronoUnit.DAYS.between(
                                period.getStartDate(), endDate) / 365.0;
                    })
                    .sum();
        }

        CareerLevel level = CareerLevel.fromCareerYears(careerYears);

        log.warn("[CareerLevelEvaluationWorkflow] Using fallback evaluation - careerYears: {}, level: {}",
                careerYears, level);

        return new CareerLevelEvaluation(
                level,
                0.5, // Low confidence for fallback
                String.format("경력 %.1f년 기준 추정", careerYears),
                List.of("경력 연수 기반 평가"),
                List.of("상세 프로필 작성 시 더 정확한 평가 가능")
        );
    }
}
