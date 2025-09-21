package com.shingu.roadmap.apis.openai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ConcurrentHashMap;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.error.OpenAiErrorHandler;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import com.shingu.roadmap.apis.openai.retry.OpenAiRetryHandler;
import com.shingu.roadmap.apis.openai.validation.OpenAiInputValidator;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;

/**
 * OpenAI Assistants v2 Client (Production Enhanced)
 * - Thread 생성/재사용 → Message 추가 → Run 생성/폴링 → Message 조회
 * - WebClient (spring-boot-starter-webflux) 기반
 * - 보안, 안정성, 성능 개선 적용
 */
@Component
@RequiredArgsConstructor
public class OpenAiClient {

  private final OpenAiConfig config;
  private final OpenAiInputValidator inputValidator;
  private final SecureLogger secureLogger;
  private final OpenAiErrorHandler errorHandler;
  private final OpenAiRetryHandler retryHandler;

  @Qualifier("openAiWebClient")
  private final WebClient openAiWebClient;
  private final ObjectMapper mapper = new ObjectMapper();

  /** Thread 캐시 (ConcurrentHashMap 기반) */
  private final ConcurrentHashMap<String, String> threadCache = new ConcurrentHashMap<>();

  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(45);
  private static final int MAX_POLL_ATTEMPTS = 10;
  private static final Duration INITIAL_POLL_DELAY = Duration.ofMillis(700);

  @PostConstruct
  private void initializeClient() {
    secureLogger.logConfigurationEvent("CLIENT_INITIALIZED",
        String.format("pollTimeout=%s, maxAttempts=%d", POLL_TIMEOUT, MAX_POLL_ATTEMPTS));
  }

  // ──────────────────────────────────────────────────────────────
  // Public API
  // ──────────────────────────────────────────────────────────────

  /** 동기(Blocking) 호출 */
  public String askAssistantBlocking(String sessionKey, String userInput) {
    return askAssistant(sessionKey, userInput).block(POLL_TIMEOUT);
  }

  /** 동기(Blocking) 호출 with timeout */
  public String askAssistantBlocking(String sessionKey, String userInput, Duration timeout) {
    return askAssistant(sessionKey, userInput).block(timeout);
  }

  /** 리액티브 호출 */
  public Mono<String> askAssistant(String sessionKey, String userInput) {
    // 입력 검증
    try {
      inputValidator.validateSessionKey(sessionKey);
      inputValidator.validateUserInput(userInput);
    } catch (Exception e) {
      secureLogger.logValidationFailure(sessionKey, "INPUT_VALIDATION", e.getMessage());
      return Mono.error(errorHandler.createException(sessionKey, "askAssistant", e));
    }

    long startTime = System.currentTimeMillis();
    Timer.Sample sample = retryHandler.startTimer("ask_assistant");

    return retryHandler.withRetry(
            ensureThread(sessionKey)
                    .flatMap(threadId -> addUserMessage(threadId, userInput).thenReturn(threadId))
                    .flatMap(this::createRunAndWait)
                    .flatMap(this::fetchLatestAssistantMessage),
            sessionKey, "ask_assistant"
    )
    .doOnSuccess(result -> {
        long duration = System.currentTimeMillis() - startTime;
        retryHandler.stopTimer(sample, "ask_assistant", true);
        secureLogger.logApiResponse(sessionKey, result != null ? result.length() : 0, duration);
        secureLogger.logResponseSummary(sessionKey, "ask_assistant", result);
    })
    .doOnError(error -> {
        retryHandler.stopTimer(sample, "ask_assistant", false);
        secureLogger.logApiError(sessionKey, "ask_assistant",
                                errorHandler.classifyError(error).name(), error.getMessage());
    });
  }

  public void invalidateThread(String sessionKey) {
    if (StringUtils.hasText(sessionKey)) {
      String threadId = threadCache.get(sessionKey);
      threadCache.remove(sessionKey);
      secureLogger.logThreadCacheEvent("INVALIDATED", sessionKey, threadId);
    }
  }

  public void invalidateAllThreads() {
    int count = threadCache.size();
    threadCache.clear();
    secureLogger.logThreadCacheEvent("INVALIDATED_ALL", "*", String.valueOf(count));
  }

  public int getThreadCacheSize() {
    return threadCache.size();
  }

  // ──────────────────────────────────────────────────────────────
  // Core Flow
  // ──────────────────────────────────────────────────────────────

  /** Thread 생성 or 캐시 재사용 */
  private Mono<String> ensureThread(String sessionKey) {
    String cachedThreadId = threadCache.get(sessionKey);
    if (cachedThreadId != null) {
      secureLogger.logCacheHit(sessionKey, "thread_cache", cachedThreadId);
      return Mono.just(cachedThreadId);
    }

    secureLogger.logCacheMiss(sessionKey, "thread_cache", sessionKey);
    return openAiWebClient.post()
            .uri("/v1/threads")
            .bodyValue(Map.of())
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::toError)
            .bodyToMono(JsonNode.class)
            .map(json -> json.path("id").asText(null))
            .flatMap(id -> {
              if (!StringUtils.hasText(id)) {
                return Mono.error(new OpenAiClientException("Failed to create thread"));
              }
              threadCache.put(sessionKey, id);
              secureLogger.logThreadCacheEvent("CREATED", sessionKey, id);
              return Mono.just(id);
            });
  }

  /** 사용자 메시지 추가 */
  private Mono<Void> addUserMessage(String threadId, String content) {
    // 콘텐츠 살마하기 및 길이 체크
    String sanitizedContent = inputValidator.sanitizeInput(content);
    if (sanitizedContent.length() > 100000) {
      return Mono.error(new IllegalArgumentException("Message content too long"));
    }

    return openAiWebClient.post()
            .uri("/v1/threads/{id}/messages", threadId)
            .bodyValue(Map.of("role", "user", "content", sanitizedContent))
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::toError)
            .toBodilessEntity()
            .then();
  }

  /** Run 생성 후 완료될 때까지 Polling */
  private Mono<String> createRunAndWait(String threadId) {
    return openAiWebClient.post()
            .uri("/v1/threads/{id}/runs", threadId)
            .bodyValue(Map.of("assistant_id", config.getNcsCodeAssistantId(),
                            "temperature", config.getTemperature()))
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::toError)
            .bodyToMono(JsonNode.class)
            .map(json -> json.path("id").asText(null))
            .flatMap(runId -> {
              if (!StringUtils.hasText(runId)) {
                return Mono.error(new OpenAiClientException("Failed to create run"));
              }
              return pollRun(threadId, runId).thenReturn(threadId);
            });
  }

  private Mono<Void> pollRun(String threadId, String runId) {
    return Mono.defer(() ->
                    openAiWebClient.get()
                            .uri("/v1/threads/{tid}/runs/{rid}", threadId, runId)
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, this::toError)
                            .bodyToMono(JsonNode.class)
                            .flatMap(run -> {
                              String status = run.path("status").asText("");
                              return switch (status) {
                                case "completed" -> {
                                  secureLogger.logPerformanceMetric("run_completed",
                                          System.currentTimeMillis(), 0);
                                  yield Mono.<Void>empty();
                                }
                                case "failed" -> {
                                  String error = run.path("last_error").path("message").asText("Run failed");
                                  yield Mono.error(new OpenAiClientException("Run failed: " + error));
                                }
                                case "expired", "cancelled" ->
                                        Mono.error(new OpenAiClientException("Run status=" + status));
                                default -> Mono.error(new ContinuePollingException());
                              };
                            })
            )
            .delaySubscription(INITIAL_POLL_DELAY)
            .retryWhen(Retry.backoff(MAX_POLL_ATTEMPTS, INITIAL_POLL_DELAY)
                    .maxBackoff(Duration.ofSeconds(4))
                    .filter(ex -> ex instanceof ContinuePollingException)
                    .doBeforeRetry(signal -> {
                      secureLogger.logRetryAttempt(threadId, "poll_run",
                              (int) signal.totalRetries() + 1, "polling");
                    }))
            .then()
            .timeout(POLL_TIMEOUT);
  }

  /** 최신 assistant 메시지 추출 */
  private Mono<String> fetchLatestAssistantMessage(String threadId) {
    return openAiWebClient.get()
            .uri("/v1/threads/{id}/messages", threadId)
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::toError)
            .bodyToMono(JsonNode.class)
            .map(this::extractAssistantTextSafely);
  }

  // ──────────────────────────────────────────────────────────────
  // Helpers
  // ──────────────────────────────────────────────────────────────

  private String extractAssistantTextSafely(JsonNode json) {
    if (json == null) return "[No response]";
    JsonNode data = json.path("data");
    if (!data.isArray() || data.isEmpty()) return "[No messages]";

    for (JsonNode msg : data) {
      if ("assistant".equals(msg.path("role").asText())) {
        String text = extractMessageText(msg);
        if (StringUtils.hasText(text)) return text;
      }
    }
    return "[Empty assistant message]";
  }

  private String extractMessageText(JsonNode msg) {
    JsonNode content = msg.path("content");
    if (content.isArray()) {
      for (JsonNode c : content) {
        JsonNode val = c.path("text").path("value");
        if (!val.isMissingNode()) {
          return val.asText();
        }
      }
    }
    return null;
  }

  private Mono<? extends Throwable> toError(ClientResponse resp) {
    return resp.bodyToMono(String.class)
            .defaultIfEmpty("")
            .map(body -> {
              String sanitizedBody = body.length() > 500 ? body.substring(0, 500) + "..." : body;
              return new OpenAiClientException(
                      "OpenAI API error: " + resp.statusCode() + " body=" + sanitizedBody
              );
            });
  }

  private static class ContinuePollingException extends RuntimeException {}

  public static class OpenAiClientException extends RuntimeException {
    public OpenAiClientException(String msg) { super(msg); }
    public OpenAiClientException(String msg, Throwable cause) { super(msg, cause); }
  }
}
