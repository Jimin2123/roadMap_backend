package com.shingu.roadmap.apis.openai.retry;

import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.error.OpenAiErrorHandler;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiRetryHandler {

    private final OpenAiConfig config;
    private final OpenAiErrorHandler errorHandler;
    private final SecureLogger secureLogger;
    private final MeterRegistry meterRegistry;

    public <T> Mono<T> withRetry(Mono<T> operation, String sessionKey, String operationName) {
        AtomicInteger attemptCounter = new AtomicInteger(0);

        return operation
                .doOnSubscribe(s -> {
                    recordAttempt(operationName);
                    secureLogger.logApiCall(sessionKey, operationName, 0);
                })
                .retryWhen(createRetrySpec(sessionKey, operationName, attemptCounter))
                .doOnSuccess(result -> {
                    recordSuccess(operationName, attemptCounter.get());
                    secureLogger.logApiResponse(sessionKey, 0, 0);
                })
                .doOnError(error -> {
                    OpenAiErrorHandler.ErrorContext context = errorHandler.createErrorContext(
                            sessionKey, operationName, error);
                    errorHandler.handleCriticalError(context);
                    recordError(operationName, context.getErrorType().name(), attemptCounter.get());
                });
    }

    public <T> Mono<T> withCustomRetry(Mono<T> operation, String sessionKey, String operationName,
                                      RetryConfiguration retryConfig) {
        AtomicInteger attemptCounter = new AtomicInteger(0);

        return operation
                .doOnSubscribe(s -> {
                    recordAttempt(operationName);
                    secureLogger.logApiCall(sessionKey, operationName, 0);
                })
                .retryWhen(createCustomRetrySpec(sessionKey, operationName, attemptCounter, retryConfig))
                .doOnSuccess(result -> {
                    recordSuccess(operationName, attemptCounter.get());
                    secureLogger.logApiResponse(sessionKey, 0, 0);
                })
                .doOnError(error -> {
                    OpenAiErrorHandler.ErrorContext context = errorHandler.createErrorContext(
                            sessionKey, operationName, error);
                    errorHandler.handleCriticalError(context);
                    recordError(operationName, context.getErrorType().name(), attemptCounter.get());
                });
    }

    private Retry createRetrySpec(String sessionKey, String operationName, AtomicInteger attemptCounter) {
        return Retry.backoff(config.getMaxRetryAttempts(), config.getInitialRetryDelay())
                .maxBackoff(config.getMaxRetryDelay())
                .jitter(0.1) // 10% 지터 추가
                .filter(this::shouldRetry)
                .doBeforeRetry(signal -> {
                    int attemptNumber = attemptCounter.incrementAndGet();
                    recordRetry(operationName, attemptNumber);

                    OpenAiErrorHandler.ErrorType errorType = errorHandler.classifyError(signal.failure());
                    Duration delay = errorHandler.getRetryDelay(errorType, attemptNumber);

                    secureLogger.logRetryAttempt(sessionKey, operationName, attemptNumber,
                                               errorType.getDescription());

                    log.warn("Retrying OpenAI operation: {} (attempt: {}, delay: {}ms, error: {})",
                           operationName, attemptNumber, delay.toMillis(), errorType);
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    OpenAiErrorHandler.ErrorContext context = errorHandler.createErrorContext(
                            sessionKey, operationName, retrySignal.failure());

                    secureLogger.logApiError(sessionKey, operationName, "RETRY_EXHAUSTED",
                                           String.format("All %d retry attempts exhausted", config.getMaxRetryAttempts()));

                    return errorHandler.createException(sessionKey, operationName, retrySignal.failure());
                });
    }

    private Retry createCustomRetrySpec(String sessionKey, String operationName,
                                       AtomicInteger attemptCounter, RetryConfiguration retryConfig) {
        return Retry.backoff(retryConfig.getMaxAttempts(), retryConfig.getInitialDelay())
                .maxBackoff(retryConfig.getMaxDelay())
                .jitter(retryConfig.getJitter())
                .filter(error -> shouldRetryWithConfig(error, retryConfig))
                .doBeforeRetry(signal -> {
                    int attemptNumber = attemptCounter.incrementAndGet();
                    recordRetry(operationName, attemptNumber);

                    OpenAiErrorHandler.ErrorType errorType = errorHandler.classifyError(signal.failure());

                    secureLogger.logRetryAttempt(sessionKey, operationName, attemptNumber,
                                               errorType.getDescription());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                        errorHandler.createException(sessionKey, operationName, retrySignal.failure()));
    }

    private boolean shouldRetry(Throwable error) {
        OpenAiErrorHandler.ErrorType errorType = errorHandler.classifyError(error);
        boolean retryable = errorHandler.isRetryable(errorType);

        if (!retryable) {
            log.debug("Error type {} is not retryable", errorType);
        }

        return retryable;
    }

    private boolean shouldRetryWithConfig(Throwable error, RetryConfiguration config) {
        OpenAiErrorHandler.ErrorType errorType = errorHandler.classifyError(error);

        if (config.getRetryableErrors().isEmpty()) {
            return errorHandler.isRetryable(errorType);
        }

        return config.getRetryableErrors().contains(errorType);
    }

    private void recordAttempt(String operation) {
        Counter.builder("openai.api.attempts")
                .tag("operation", operation)
                .description("Total number of OpenAI API attempts")
                .register(meterRegistry)
                .increment();
    }

    private void recordRetry(String operation, int attemptNumber) {
        Counter.builder("openai.api.retries")
                .tag("operation", operation)
                .tag("attempt", String.valueOf(attemptNumber))
                .description("Number of OpenAI API retries")
                .register(meterRegistry)
                .increment();
    }

    private void recordSuccess(String operation, int totalAttempts) {
        Counter.builder("openai.api.success")
                .tag("operation", operation)
                .description("Number of successful OpenAI API calls")
                .register(meterRegistry)
                .increment();

        if (totalAttempts > 1) {
            Counter.builder("openai.api.success_after_retry")
                    .tag("operation", operation)
                    .tag("total_attempts", String.valueOf(totalAttempts))
                    .description("Number of successful OpenAI API calls after retry")
                    .register(meterRegistry)
                    .increment();
        }
    }

    private void recordError(String operation, String errorType, int totalAttempts) {
        Counter.builder("openai.api.errors")
                .tag("operation", operation)
                .tag("error_type", errorType)
                .tag("total_attempts", String.valueOf(totalAttempts))
                .description("Number of failed OpenAI API calls")
                .register(meterRegistry)
                .increment();
    }

    public static class RetryConfiguration {
        private final int maxAttempts;
        private final Duration initialDelay;
        private final Duration maxDelay;
        private final double jitter;
        private final java.util.Set<OpenAiErrorHandler.ErrorType> retryableErrors;

        private RetryConfiguration(Builder builder) {
            this.maxAttempts = builder.maxAttempts;
            this.initialDelay = builder.initialDelay;
            this.maxDelay = builder.maxDelay;
            this.jitter = builder.jitter;
            this.retryableErrors = builder.retryableErrors;
        }

        public static Builder builder() {
            return new Builder();
        }

        public int getMaxAttempts() { return maxAttempts; }
        public Duration getInitialDelay() { return initialDelay; }
        public Duration getMaxDelay() { return maxDelay; }
        public double getJitter() { return jitter; }
        public java.util.Set<OpenAiErrorHandler.ErrorType> getRetryableErrors() { return retryableErrors; }

        public static class Builder {
            private int maxAttempts = 3;
            private Duration initialDelay = Duration.ofSeconds(1);
            private Duration maxDelay = Duration.ofSeconds(60);
            private double jitter = 0.1;
            private java.util.Set<OpenAiErrorHandler.ErrorType> retryableErrors = java.util.Set.of();

            public Builder maxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
                return this;
            }

            public Builder initialDelay(Duration initialDelay) {
                this.initialDelay = initialDelay;
                return this;
            }

            public Builder maxDelay(Duration maxDelay) {
                this.maxDelay = maxDelay;
                return this;
            }

            public Builder jitter(double jitter) {
                this.jitter = jitter;
                return this;
            }

            public Builder retryableErrors(java.util.Set<OpenAiErrorHandler.ErrorType> retryableErrors) {
                this.retryableErrors = retryableErrors;
                return this;
            }

            public RetryConfiguration build() {
                return new RetryConfiguration(this);
            }
        }
    }

    public Timer.Sample startTimer(String operation) {
        return Timer.start(meterRegistry);
    }

    public void stopTimer(Timer.Sample sample, String operation, boolean success) {
        if (sample != null) {
            sample.stop(Timer.builder("openai.api.duration")
                    .tag("operation", operation)
                    .tag("success", String.valueOf(success))
                    .description("Duration of OpenAI API calls")
                    .register(meterRegistry));
        }
    }
}