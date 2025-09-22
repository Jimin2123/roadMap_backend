package com.shingu.roadmap.apis.openai.error;

import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiErrorHandler {

  private final SecureLogger secureLogger;

  public enum ErrorType {
    RATE_LIMIT("Rate limit exceeded"),
    QUOTA_EXCEEDED("API quota exceeded"),
    INVALID_REQUEST("Invalid request format"),
    AUTHENTICATION_ERROR("Authentication failed"),
    NETWORK_ERROR("Network connectivity issue"),
    TIMEOUT("Request timeout"),
    INTERNAL_ERROR("Internal server error"),
    VALIDATION_ERROR("Input validation failed"),
    CONFIGURATION_ERROR("Configuration error"),
    UNKNOWN_ERROR("Unknown error");

    private final String description;

    ErrorType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  public ErrorType classifyError(Throwable error) {
    if (error instanceof WebClientResponseException ex) {
      return classifyHttpError(ex);
    }

    if (error instanceof TimeoutException) {
      return ErrorType.TIMEOUT;
    }

    if (error instanceof ConnectException || error instanceof UnknownHostException) {
      return ErrorType.NETWORK_ERROR;
    }

    if (error instanceof IllegalArgumentException || error instanceof SecurityException) {
      return ErrorType.VALIDATION_ERROR;
    }

    if (error instanceof IllegalStateException) {
      return ErrorType.CONFIGURATION_ERROR;
    }

    return ErrorType.UNKNOWN_ERROR;
  }

  private ErrorType classifyHttpError(WebClientResponseException ex) {
    return switch (ex.getStatusCode().value()) {
      case 400 -> ErrorType.INVALID_REQUEST;
      case 401, 403 -> ErrorType.AUTHENTICATION_ERROR;
      case 402 -> ErrorType.QUOTA_EXCEEDED;
      case 429 -> ErrorType.RATE_LIMIT;
      case 500, 502, 503 -> ErrorType.INTERNAL_ERROR;
      case 504 -> ErrorType.TIMEOUT;
      default -> ErrorType.UNKNOWN_ERROR;
    };
  }

  public Duration getRetryDelay(ErrorType errorType, int attemptNumber) {
    return switch (errorType) {
      case RATE_LIMIT -> {
        // 지수 백오프: 2^attemptNumber초, 최대 60초
        long delay = Math.min(60, (long) Math.pow(2, attemptNumber));
        yield Duration.ofSeconds(delay);
      }
      case NETWORK_ERROR, TIMEOUT -> {
        // 선형 증가: attemptNumber * 5초, 최대 30초
        long delay = Math.min(30, attemptNumber * 5L);
        yield Duration.ofSeconds(delay);
      }
      case INTERNAL_ERROR -> {
        // 중간 수준의 백오프: attemptNumber * 2초, 최대 10초
        long delay = Math.min(10, attemptNumber * 2L);
        yield Duration.ofSeconds(delay);
      }
      default -> Duration.ZERO; // 재시도 불가
    };
  }

  public boolean isRetryable(ErrorType errorType) {
    return Set.of(
            ErrorType.RATE_LIMIT,
            ErrorType.NETWORK_ERROR,
            ErrorType.TIMEOUT,
            ErrorType.INTERNAL_ERROR
    ).contains(errorType);
  }

  public boolean isTemporary(ErrorType errorType) {
    return Set.of(
            ErrorType.RATE_LIMIT,
            ErrorType.NETWORK_ERROR,
            ErrorType.TIMEOUT,
            ErrorType.INTERNAL_ERROR
    ).contains(errorType);
  }

  public boolean isCritical(ErrorType errorType) {
    return Set.of(
            ErrorType.QUOTA_EXCEEDED,
            ErrorType.AUTHENTICATION_ERROR,
            ErrorType.CONFIGURATION_ERROR
    ).contains(errorType);
  }

  public OpenAiException createException(String sessionKey, String operation, Throwable originalError) {
    ErrorType errorType = classifyError(originalError);
    String errorMessage = buildErrorMessage(errorType, originalError);

    secureLogger.logApiError(sessionKey, operation, errorType.name(), errorMessage);

    return new OpenAiException(
            errorMessage,
            originalError,
            errorType,
            isRetryable(errorType),
            isTemporary(errorType),
            isCritical(errorType)
    );
  }

  private String buildErrorMessage(ErrorType errorType, Throwable originalError) {
    StringBuilder message = new StringBuilder();
    message.append(errorType.getDescription());

    if (originalError instanceof WebClientResponseException ex) {
      message.append(" (HTTP ").append(ex.getStatusCode().value()).append(")");

      // 에러 응답 본문이 있으면 포함 (민감한 정보는 제외)
      String responseBody = ex.getResponseBodyAsString();
      if (responseBody != null && !responseBody.isEmpty() && responseBody.length() < 500) {
        message.append(": ").append(sanitizeErrorResponse(responseBody));
      }
    } else if (originalError.getMessage() != null) {
      message.append(": ").append(originalError.getMessage());
    }

    return message.toString();
  }

  private String sanitizeErrorResponse(String responseBody) {
    // OpenAI API 에러 응답에서 민감한 정보 제거
    return responseBody
            .replaceAll("(?i)(api[_-]?key|token|secret)\"?:\\s*\"?[^\"\\s,}]+", "$1\": \"****\"")
            .replaceAll("\"sk-[^\"]+\"", "\"sk-****\"");
  }

  public ErrorContext createErrorContext(String sessionKey, String operation, Throwable error) {
    ErrorType errorType = classifyError(error);

    return ErrorContext.builder()
            .sessionKey(sessionKey)
            .operation(operation)
            .errorType(errorType)
            .originalError(error)
            .retryable(isRetryable(errorType))
            .temporary(isTemporary(errorType))
            .critical(isCritical(errorType))
            .errorMessage(buildErrorMessage(errorType, error))
            .timestamp(System.currentTimeMillis())
            .build();
  }

  public void handleCriticalError(ErrorContext context) {
    if (context.isCritical()) {
      secureLogger.logSecurityEvent(
              context.getSessionKey(),
              "CRITICAL_ERROR",
              String.format("Critical error in operation %s: %s",
                      context.getOperation(), context.getErrorType())
      );

      // 필요시 알림 시스템 호출
      notifyOperationsTeam(context);
    }
  }

  private void notifyOperationsTeam(ErrorContext context) {
    // 운영팀 알림 로직 (예: Slack, email 등)
    log.error("CRITICAL ALERT: OpenAI API critical error - Operation: {}, Error: {}",
            context.getOperation(), context.getErrorType());
  }

  public static class OpenAiException extends RuntimeException {
    private final ErrorType errorType;
    private final boolean retryable;
    private final boolean temporary;
    private final boolean critical;

    public OpenAiException(String message, Throwable cause, ErrorType errorType,
                           boolean retryable, boolean temporary, boolean critical) {
      super(message, cause);
      this.errorType = errorType;
      this.retryable = retryable;
      this.temporary = temporary;
      this.critical = critical;
    }

    public ErrorType getErrorType() { return errorType; }
    public boolean isRetryable() { return retryable; }
    public boolean isTemporary() { return temporary; }
    public boolean isCritical() { return critical; }
  }

  @lombok.Builder
  @lombok.Data
  public static class ErrorContext {
    private String sessionKey;
    private String operation;
    private ErrorType errorType;
    private Throwable originalError;
    private boolean retryable;
    private boolean temporary;
    private boolean critical;
    private String errorMessage;
    private long timestamp;
  }
}