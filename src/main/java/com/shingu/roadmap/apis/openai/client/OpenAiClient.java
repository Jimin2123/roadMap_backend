package com.shingu.roadmap.apis.openai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

  private final OpenAiConfig config;

  @Qualifier("openAiWebClient")
  private final WebClient openAiWebClient;

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

    return openAiWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> json.path("choices").get(0).path("message").path("content").asText());
  }

  public Mono<String> generateAssistantResponse(String userInput) {
    return openAiWebClient.post()
            .uri("/threads")
            .body(BodyInserters.fromValue(Collections.emptyMap()))
            .retrieve()
            .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(String.class).flatMap(body -> {
                      log.error("OpenAI /threads 호출 실패: {}", body);
                      return Mono.error(new RuntimeException("OpenAI 오류: " + body));
                    })
            )
            .bodyToMono(JsonNode.class)
            .map(json -> json.get("id").asText())
            .flatMap(threadId -> {
              return openAiWebClient.post()
                      .uri("/threads/" + threadId + "/messages")
                      .bodyValue(Map.of(
                              "role", "user",
                              "content", userInput
                      ))
                      .retrieve()
                      .bodyToMono(JsonNode.class)
                      .thenReturn(threadId);
            })
            .flatMap(threadId -> {
              return openAiWebClient.post()
                      .uri("/threads/" + threadId + "/runs")
                      .bodyValue(Map.of("assistant_id", config.getNcsCodeAssistantId()))
                      .retrieve()
                      .bodyToMono(JsonNode.class)
                      .map(json -> Map.entry(threadId, json.get("id").asText()));
            })
            .flatMap(entry -> waitForCompletion(entry.getKey(), entry.getValue()));
  }

  private Mono<String> waitForCompletion(String threadId, String runId) {
    return Mono.defer(() ->
            openAiWebClient.get()
                    .uri("/threads/" + threadId + "/runs/" + runId)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
    ).flatMap(json -> {
      String status = json.get("status").asText();
      if ("completed".equals(status)) {
        return getMessagesFromThread(threadId);
      } else if ("failed".equals(status)) {
        return Mono.error(new IllegalStateException("Assistant run failed"));
      } else {
        return Mono.delay(Duration.ofSeconds(1)).then(waitForCompletion(threadId, runId));
      }
    });
  }

  private Mono<String> getMessagesFromThread(String threadId) {
    return openAiWebClient.get()
            .uri("/threads/" + threadId + "/messages")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> {
              JsonNode data = json.get("data");
              for (JsonNode message : data) {
                if ("assistant".equals(message.get("role").asText())) {
                  return message.get("content").get(0).get("text").get("value").asText();
                }
              }
              return "[No assistant response]";
            });
  }

}