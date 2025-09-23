package com.shingu.roadmap.apis.openai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.error.OpenAiErrorHandler;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
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
import java.util.concurrent.TimeoutException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

  private final OpenAiConfig config;
  private final OpenAiErrorHandler errorHandler;
  private final SecureLogger secureLogger;

  @Qualifier("openAiWebClient")
  private final WebClient openAiWebClient;

  /**
   * OpenAI Chat Completion API 호출
   * @param messages system/user/assistant 역할의 message 목록
   * @return OpenAI가 생성한 응답 메시지 텍스트
   */
  public Mono<String> generateChatCompletion(List<Map<String, String>> messages) {
    String sessionKey = generateSessionKey();
    String operation = "generateChatCompletion";

    secureLogger.logApiCall(sessionKey, "/chat/completions",
        messages.toString().length());

    Map<String, Object> requestBody = Map.of(
            "model", config.getModel(),
            "temperature", config.getTemperature(),
            "messages", messages
    );

    long startTime = System.currentTimeMillis();

    return openAiWebClient.post()
            .uri("/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> {
              String response = json.path("choices").get(0).path("message").path("content").asText();
              long duration = System.currentTimeMillis() - startTime;
              secureLogger.logApiResponse(sessionKey, response.length(), duration);
              return response;
            })
            .onErrorMap(throwable -> {
              OpenAiErrorHandler.OpenAiException exception = errorHandler.createException(
                  sessionKey, operation, throwable);
              errorHandler.handleCriticalError(errorHandler.createErrorContext(
                  sessionKey, operation, throwable));
              return exception;
            });
  }

  public Mono<String> generateAssistantResponse(String userInput) {
    String sessionKey = generateSessionKey();
    String operation = "generateAssistantResponse";

    secureLogger.logApiCall(sessionKey, "/threads", userInput.length());
    long startTime = System.currentTimeMillis();

    return openAiWebClient.post()
            .uri("/threads")
            .body(BodyInserters.fromValue(Collections.emptyMap()))
            .retrieve()
            .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(String.class).flatMap(body -> {
                      secureLogger.logApiError(sessionKey, operation + "_create_thread", "HTTP_ERROR", body);
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
            .flatMap(entry -> waitForCompletion(entry.getKey(), entry.getValue(), sessionKey, operation))
            .timeout(Duration.ofSeconds(60))
            .map(response -> {
              long duration = System.currentTimeMillis() - startTime;
              secureLogger.logApiResponse(sessionKey, response.length(), duration);
              return response;
            })
            .onErrorMap(throwable -> {
              OpenAiErrorHandler.OpenAiException exception = errorHandler.createException(
                  sessionKey, operation, throwable);
              errorHandler.handleCriticalError(errorHandler.createErrorContext(
                  sessionKey, operation, throwable));
              return exception;
            });
  }

  private Mono<String> waitForCompletion(String threadId, String runId, String sessionKey, String operation) {
    return Mono.defer(() ->
                    openAiWebClient.get()
                            .uri("/threads/" + threadId + "/runs/" + runId)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
            )
            .flatMap(json -> {
              String status = json.get("status").asText();
              if ("completed".equals(status)) {
                return getMessagesFromThread(threadId, sessionKey, operation);
              } else if ("failed".equals(status) || "cancelled".equals(status) || "expired".equals(status)) {
                secureLogger.logApiError(sessionKey, operation, "RUN_FAILED", "Status: " + status);
                return Mono.error(new IllegalStateException("Assistant run 실패. Status: " + status));
              } else {
                // 상태에 따른 적응형 딜레이: 처리 중일 때는 더 짧은 간격으로 체크
                Duration delay = "in_progress".equals(status) ?
                    Duration.ofMillis(500) : Duration.ofSeconds(1);
                return Mono.delay(delay).then(waitForCompletion(threadId, runId, sessionKey, operation));
              }
            })
            .onErrorMap(throwable -> {
              if (!(throwable instanceof IllegalStateException)) {
                OpenAiErrorHandler.OpenAiException exception = errorHandler.createException(
                    sessionKey, operation + "_wait_completion", throwable);
                errorHandler.handleCriticalError(errorHandler.createErrorContext(
                    sessionKey, operation + "_wait_completion", throwable));
                return exception;
              }
              return throwable;
            });
  }

  private Mono<String> getMessagesFromThread(String threadId, String sessionKey, String operation) {
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
            })
            .onErrorMap(throwable -> {
              OpenAiErrorHandler.OpenAiException exception = errorHandler.createException(
                  sessionKey, operation + "_get_messages", throwable);
              errorHandler.handleCriticalError(errorHandler.createErrorContext(
                  sessionKey, operation + "_get_messages", throwable));
              return exception;
            });
  }

  private String generateSessionKey() {
    return "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
  }
}