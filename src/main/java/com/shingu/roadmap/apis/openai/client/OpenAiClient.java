package com.shingu.roadmap.apis.openai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

  private final OpenAiConfig config;

  @Qualifier("openAiWebClient")
  private final WebClient webClient;

  /**
   * OpenAI Chat Completion API 호출
   * @param messages system/user/assistant 역할의 message 목록
   * @return OpenAI가 생성한 응답 메시지 텍스트
   */
  public Mono<String> generateChatCompletion(List<Map<String, String>> messages) {
    Map<String, Object> requestBody = Map.of(
            "model", config.getModel(),
            "temperature", config.getTemperature(),
            "messages", messages
    );

    return webClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> json.path("choices").get(0).path("message").path("content").asText());
  }
}