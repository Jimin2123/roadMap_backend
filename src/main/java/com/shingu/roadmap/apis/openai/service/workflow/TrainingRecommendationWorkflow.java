package com.shingu.roadmap.apis.openai.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiCacheConfig;
import com.shingu.roadmap.apis.openai.dto.request.GptUserPromptRequest;
import com.shingu.roadmap.apis.openai.dto.request.GptUserProfileDto;
import com.shingu.roadmap.apis.openai.dto.request.TrainingRecommendationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 훈련과정 추천 워크플로우
 * 사용자 프로필 기반 훈련과정 추천을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingRecommendationWorkflow {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    /**
     * 사용자 정보를 바탕으로 부족한 역량을 보완할 훈련과정을 추천합니다.
     * (AI가 주어진 목록 내에서만 응답하도록 프롬프트를 강화했습니다.)
     */
    @Cacheable(value = OpenAiCacheConfig.TRAINING_RECOMMENDATION_CACHE, keyGenerator = "openAiCacheKeyGenerator")
    public Mono<Set<String>> recommendTrainingCourse(TrainingRecommendationRequest request) {
        if (request == null || request.userProfile() == null || request.trainingCourses() == null) {
            return Mono.error(new IllegalArgumentException("요청 정보가 올바르지 않습니다."));
        }

        String systemPrompt = """
            당신은 사용자의 프로필과 희망 직무를 분석하여, 역량 강화를 위한 최적의 훈련과정을 추천하는 AI입니다.

            [규칙]
            1. 당신은 **반드시** 입력으로 주어진 [훈련과정 리스트] 내에 존재하는 과정 중에서만 추천해야 합니다.
            2. 사용자의 현재 보유 역량(기술, 자격증)과 희망 직무(desiredJob, NCS 코드)를 비교하여, 부족한 부분을 채워줄 수 있는 과정을 선택합니다.
            3. 사용자의 주소(address)를 참고하여 지역적으로 수강 가능한 훈련과정을 우선적으로 고려해야 합니다.
            4. 이미 보유한 역량과 중복되는 과정은 추천하지 않습니다.

            [출력 형식]
            - 반드시 아래 JSON 배열 형식으로만 응답해주세요.
            - 예시: ["T123456", "T654321"]
            - 설명이나 다른 텍스트는 절대 포함하지 마세요.
            """;

        String userPrompt;
        try {
            GptUserPromptRequest promptRequest = new GptUserPromptRequest(
                    GptUserProfileDto.from(request.userProfile()),
                    request.address(),
                    request.trainingCourses()
            );
            userPrompt = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(promptRequest);

        } catch (JsonProcessingException e) {
            log.error("User Prompt JSON 직렬화 실패", e);
            return Mono.error(new RuntimeException("요청 직렬화 실패", e));
        }

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        return openAiClient.generateChatCompletion(messages)
                .flatMap(response -> {
                    try {
                        Set<String> ids = objectMapper.readValue(response, new TypeReference<Set<String>>() {});
                        return Mono.just(ids);
                    } catch (JsonProcessingException e) {
                        log.error("GPT 응답 파싱 실패: {}", response, e);
                        return Mono.error(new RuntimeException("GPT 응답 파싱 오류", e));
                    }
                });
    }
}
