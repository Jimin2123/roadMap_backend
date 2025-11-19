package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.careernet.service.CareerNetCodeProvider;
import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.util.ResumeTextFormatter;
import com.shingu.roadmap.member.domain.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 커리어넷 검색 코드 추천 워크플로우
 * 사용자 프로필 기반 커리어넷 API 검색 코드를 추천합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareerNetSearchCodeWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final CareerNetCodeProvider careerNetCodeProvider;
    private final ResumeTextFormatter resumeTextFormatter;

    /**
     * 사용자의 상세 정보를 바탕으로 커리어넷 API 검색에 적합한 분류 코드를 추천합니다.
     * (AI가 주어진 목록 내에서만 응답하도록 프롬프트를 강화했습니다.)
     */
    @Cacheable(value = OpenAiCacheConfig.SEARCH_CODES_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<Map<String, String>> recommendSearchCodes(Profile profile) {
        String jobInfoCodesJson = careerNetCodeProvider.getJobInfoCodesJson();
        String encyclopediaCodesJson = careerNetCodeProvider.getEncyclopediaCodesJson();

        String userContext = """
            - 보유 기술: %s
            - 보유 자격증: %s
            - 보유 NCS 역량 : %s
            - 희망 직무 NCS 역량 : %s
            - 이력서 내용:
            %s
        """.formatted(
                profile.getProfileSkills().stream().map(ps -> ps.getSkill().getName()).collect(Collectors.joining(", ")),
                profile.getResume() != null ? profile.getResume().getCertificates().stream().map(rc -> rc.getCertificate().getJmfldnm()).collect(Collectors.joining(", ")) : "",
                profile.getUserCapabilities().stream().map(NcsOccupation::getDutyNm).collect(Collectors.joining(", ")),
                profile.getDesiredCapabilities().stream().map(NcsOccupation::getDutyNm).collect(Collectors.joining(", ")),
                resumeTextFormatter.resumeToText(profile.getResume())
        );

        String systemPrompt = """
            당신은 사용자의 프로필을 분석하여 커리어넷 API 검색에 가장 적합한 분류 코드를 추천하는 고도로 정확한 AI입니다.

            [규칙]
            1. 당신은 **반드시** [사용자 정보]만을 근거로 판단해야 합니다.
            2. 당신은 **반드시** [선택 가능한 코드 목록]에 명시된 코드 중에서만 각 분류별로 가장 적합한 코드 **하나만**을 선택해야 합니다.

            [출력 형식]
            - 반드시 아래 JSON 형식으로만 응답해야 합니다.
            - 설명, ملاحظات, 마크다운 기호("```json") 등 다른 어떤 텍스트도 포함해서는 안 됩니다.

            {
              "jobInfoCategoryCode": "...",
              "jobInfoAbilityCode": "...",
              "encyclopediaThemeCode": "..."
            }
            """;

        String userPrompt = """
              [사용자 정보]
              %s

              [선택 가능한 '직업 정보' 및 '직업 능력' 코드 목록]
              %s

              [선택 가능한 '직업 백과' 테마 목록]
              %s
              """.formatted(userContext, jobInfoCodesJson, encyclopediaCodesJson);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .flatMap(resp -> {
                    try {
                        String raw = resp.trim();
                        if (raw.startsWith("```")) {
                            raw = raw.replaceAll("```json", "").replaceAll("```", "").trim();
                        }
                        Map<String, String> codes = objectMapper.readValue(raw, new TypeReference<>() {});
                        return Mono.just(Objects.requireNonNullElse(codes, Collections.emptyMap()));
                    } catch (JsonProcessingException e) {
                        log.error("GPT 응답 JSON 파싱 실패: {}", resp, e);
                        return Mono.error(new IllegalStateException("GPT 응답 파싱 실패", e));
                    }
                });
    }
}
