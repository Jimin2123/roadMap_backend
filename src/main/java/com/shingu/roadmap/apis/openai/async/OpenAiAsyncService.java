package com.shingu.roadmap.apis.openai.async;

import com.shingu.roadmap.apis.openai.cache.OpenAiCacheService;
import com.shingu.roadmap.apis.openai.client.OpenAiClient;
import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import com.shingu.roadmap.apis.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiAsyncService {

    private final OpenAiService openAiService;
    private final OpenAiClient openAiClient;
    private final OpenAiCacheService cacheService;
    private final OpenAiConfig config;
    private final SecureLogger secureLogger;

    /**
     * 병렬 처리로 여러 추천 작업을 동시 실행
     */
    public Mono<BatchRecommendationResult> batchRecommendations(BatchRecommendationRequest request) {
        String sessionKey = "batch-reco:" + System.currentTimeMillis();
        secureLogger.logApiCall(sessionKey, "batchRecommendations", request.getRequests().size());

        long startTime = System.currentTimeMillis();

        List<Mono<RecommendationResult>> tasks = request.getRequests().stream()
                .map(req -> processRecommendation(req)
                        .subscribeOn(Schedulers.boundedElastic()) // 병렬 처리
                        .timeout(Duration.ofSeconds(30)) // 개별 작업 타임아웃
                        .onErrorResume(error -> {
                            secureLogger.logApiError(sessionKey, "batchRecommendation",
                                                   "INDIVIDUAL_TASK_ERROR", error.getMessage());
                            return Mono.just(RecommendationResult.error(req.getType(), error.getMessage()));
                        }))
                .toList();

        return Flux.fromIterable(tasks)
                .flatMap(task -> task, config.getMaxConnections() / 2) // 동시 실행 제한
                .collectList()
                .map(results -> BatchRecommendationResult.builder()
                        .results(results)
                        .totalCount(results.size())
                        .successCount((int) results.stream().filter(RecommendationResult::isSuccess).count())
                        .duration(System.currentTimeMillis() - startTime)
                        .build())
                .doOnSuccess(result -> {
                    secureLogger.logApiResponse(sessionKey, result.getSuccessCount(),
                                              result.getDuration());
                    secureLogger.logPerformanceMetric("batchRecommendations",
                                                     result.getDuration(), 0);
                });
    }

    /**
     * 개별 추천 작업 처리
     */
    private Mono<RecommendationResult> processRecommendation(RecommendationRequest request) {
        String cacheKey = cacheService.generateCacheKey(request.getType(), request.getParams());

        return cacheService.getCachedResponse(
                request.getType(),
                cacheKey,
                () -> executeRecommendation(request),
                request.isCacheable()
        ).map(response -> RecommendationResult.success(request.getType(), response))
         .onErrorMap(error -> new RecommendationException(request.getType(), error));
    }

    /**
     * 실제 추천 로직 실행
     */
    private Mono<String> executeRecommendation(RecommendationRequest request) {
        return switch (request.getType()) {
            case "ncs_codes" -> executeNcsCodeRecommendation(request);
            case "keywords" -> executeKeywordGeneration(request);
            case "career_codes" -> executeCareerCodeRecommendation(request);
            default -> Mono.error(new IllegalArgumentException("Unsupported recommendation type: " + request.getType()));
        };
    }

    private Mono<String> executeNcsCodeRecommendation(RecommendationRequest request) {
        // 실제 구현에서는 OpenAiService의 메서드 호출
        String sessionKey = "ncs-async:" + request.hashCode();
        return openAiClient.askAssistant(sessionKey, request.getPrompt());
    }

    private Mono<String> executeKeywordGeneration(RecommendationRequest request) {
        String sessionKey = "keyword-async:" + request.hashCode();
        return openAiClient.askAssistant(sessionKey, request.getPrompt());
    }

    private Mono<String> executeCareerCodeRecommendation(RecommendationRequest request) {
        String sessionKey = "career-async:" + request.hashCode();
        return openAiClient.askAssistant(sessionKey, request.getPrompt());
    }

    /**
     * 비동기 추천 작업 (Fire-and-Forget)
     */
    @Async("openAiTaskExecutor")
    public CompletableFuture<Void> scheduleRecommendationUpdate(String userId, String operation) {
        String sessionKey = "scheduled:" + userId + ":" + operation;
        secureLogger.logApiCall(sessionKey, "scheduleRecommendationUpdate", 0);

        return CompletableFuture.runAsync(() -> {
            try {
                // 사용자별 추천 데이터 갱신 로직
                updateUserRecommendations(userId, operation);
                secureLogger.logApiResponse(sessionKey, 1, 0);
            } catch (Exception e) {
                secureLogger.logApiError(sessionKey, "scheduleRecommendationUpdate",
                                       "ASYNC_UPDATE_ERROR", e.getMessage());
            }
        });
    }

    private void updateUserRecommendations(String userId, String operation) {
        // 실제 구현에서는 사용자 데이터 기반으로 추천 갱신
        log.debug("Updating recommendations for user: {}, operation: {}", userId, operation);
    }

    /**
     * 스트리밍 처리를 위한 리액티브 스트림
     */
    public Flux<RecommendationResult> streamRecommendations(List<RecommendationRequest> requests) {
        return Flux.fromIterable(requests)
                .flatMap(this::processRecommendation, config.getMaxConnections() / 4) // 동시성 제한
                .delayElements(Duration.ofMillis(100)) // 백프레셰 제어
                .doOnNext(result -> {
                    secureLogger.logApiResponse("stream:" + result.getType(), 1, 0);
                })
                .doOnError(error -> {
                    secureLogger.logApiError("stream", "streamRecommendations",
                                           "STREAM_ERROR", error.getMessage());
                });
    }

    /**
     * 요청 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class BatchRecommendationRequest {
        private List<RecommendationRequest> requests;
    }

    @lombok.Data
    @lombok.Builder
    public static class RecommendationRequest {
        private String type;
        private String prompt;
        private Object params;
        @lombok.Builder.Default
        private boolean cacheable = true;
    }

    /**
     * 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class BatchRecommendationResult {
        private List<RecommendationResult> results;
        private int totalCount;
        private int successCount;
        private long duration;
    }

    @lombok.Data
    @lombok.Builder
    public static class RecommendationResult {
        private String type;
        private String result;
        private boolean success;
        private String errorMessage;

        public static RecommendationResult success(String type, String result) {
            return RecommendationResult.builder()
                    .type(type)
                    .result(result)
                    .success(true)
                    .build();
        }

        public static RecommendationResult error(String type, String errorMessage) {
            return RecommendationResult.builder()
                    .type(type)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }

    /**
     * 커스텀 예외
     */
    public static class RecommendationException extends RuntimeException {
        private final String type;

        public RecommendationException(String type, Throwable cause) {
            super("Recommendation failed for type: " + type, cause);
            this.type = type;
        }

        public String getType() { return type; }
    }
}