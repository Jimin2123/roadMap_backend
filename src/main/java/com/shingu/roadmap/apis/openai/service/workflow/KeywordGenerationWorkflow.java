package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.member.domain.Profile;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 키워드 생성 워크플로우
 * 사용자 프로필 기반 커리어 키워드를 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordGenerationWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    /**
     * 사용자 프로필 기반 커리어 키워드 생성
     */
    @Cacheable(value = OpenAiCacheConfig.KEYWORD_GENERATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<Set<String>> generateKeyword(Profile profile) {
        ProfileResponse dto = ProfileResponse.from(profile);
        String userJson;
        try {
            userJson = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("사용자 JSON 직렬화 실패", e));
        }

        String systemPrompt = """
            당신은 커리어 분석 전문가입니다.
            아래 사용자 정보를 보고, 이 사람의 직무/역량에 적합한 핵심 키워드 목록을 한국어로 생성해주세요.

            [규칙]
            - **반드시** 주어진 사용자 정보에 명시된 내용(기술 스택, 주요 역할, 성과 등)만을 기반으로 키워드를 생성해야 합니다.
            - 언급되지 않은 내용은 절대 추론하지 마세요.
            - 최대 10개 이내로, 중복 없이 가장 핵심적인 키워드만 선택하세요.

            [출력 형식]
            - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
            - 예시: ["Java", "Spring Boot", "백엔드 개발", "REST API", "성능 최적화", "Docker"]
            """;

        String userPrompt = String.format("{\"user\": %s}", userJson);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .flatMap(response -> {
                    try {
                        return Mono.just(objectMapper.readValue(response, new TypeReference<Set<String>>() {}));
                    } catch (JsonProcessingException e) {
                        log.error("GPT 응답 파싱 실패: {}", response, e);
                        return Mono.error(new RuntimeException("GPT 응답 파싱 오류", e));
                    }
                });
    }
}
