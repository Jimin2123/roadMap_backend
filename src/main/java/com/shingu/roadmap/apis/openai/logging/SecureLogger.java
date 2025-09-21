package com.shingu.roadmap.apis.openai.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Slf4j
@Component
public class SecureLogger {

    private static final String MASK_PATTERN = "****";
    private static final int VISIBLE_CHARS = 4;
    private static final Pattern SENSITIVE_DATA_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|password|secret|token|bearer|auth)\\s*[:=]\\s*['\"]?([^\\s'\"]+)"
    );

    public void logApiCall(String sessionKey, String endpoint, int promptLength) {
        log.info("OpenAI API call initiated - session: {}, endpoint: {}, promptLength: {}",
                maskSensitiveData(sessionKey), endpoint, promptLength);
    }

    public void logApiResponse(String sessionKey, int responseLength, long duration) {
        log.info("OpenAI API response received - session: {}, responseLength: {}, duration: {}ms",
                maskSensitiveData(sessionKey), responseLength, duration);
    }

    public void logApiError(String sessionKey, String operation, String errorType, String errorMessage) {
        log.error("OpenAI API error - session: {}, operation: {}, errorType: {}, message: {}",
                maskSensitiveData(sessionKey), operation, errorType, sanitizeErrorMessage(errorMessage));
    }

    public void logRetryAttempt(String sessionKey, String operation, int attemptNumber, String reason) {
        log.warn("OpenAI API retry attempt - session: {}, operation: {}, attempt: {}, reason: {}",
                maskSensitiveData(sessionKey), operation, attemptNumber, reason);
    }

    public void logCacheHit(String sessionKey, String operation, String cacheKey) {
        log.debug("OpenAI cache hit - session: {}, operation: {}, cacheKey: {}",
                maskSensitiveData(sessionKey), operation, maskSensitiveData(cacheKey));
    }

    public void logCacheMiss(String sessionKey, String operation, String cacheKey) {
        log.debug("OpenAI cache miss - session: {}, operation: {}, cacheKey: {}",
                maskSensitiveData(sessionKey), operation, maskSensitiveData(cacheKey));
    }

    public void logPerformanceMetric(String operation, long duration, int tokenUsage) {
        log.info("OpenAI performance metric - operation: {}, duration: {}ms, tokens: {}",
                operation, duration, tokenUsage);
    }

    public void logConfigurationEvent(String event, String details) {
        log.info("OpenAI configuration event - event: {}, details: {}",
                event, sanitizeConfigDetails(details));
    }

    public void logSecurityEvent(String sessionKey, String event, String details) {
        log.warn("OpenAI security event - session: {}, event: {}, details: {}",
                maskSensitiveData(sessionKey), event, details);
    }

    public void logValidationFailure(String sessionKey, String validationType, String reason) {
        log.warn("OpenAI input validation failed - session: {}, type: {}, reason: {}",
                maskSensitiveData(sessionKey), validationType, reason);
    }

    public void logThreadCacheEvent(String event, String sessionKey, String threadId) {
        log.debug("OpenAI thread cache event - event: {}, session: {}, thread: {}",
                event, maskSensitiveData(sessionKey), maskSensitiveData(threadId));
    }

    private String maskSensitiveData(String data) {
        if (!StringUtils.hasText(data)) {
            return MASK_PATTERN;
        }

        if (data.length() <= VISIBLE_CHARS * 2) {
            return MASK_PATTERN;
        }

        return data.substring(0, VISIBLE_CHARS) + MASK_PATTERN +
               data.substring(data.length() - VISIBLE_CHARS);
    }

    private String sanitizeErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return errorMessage;
        }

        // API 키나 기타 민감한 정보가 포함된 에러 메시지를 마스킹
        return SENSITIVE_DATA_PATTERN.matcher(errorMessage)
                .replaceAll(match -> match.group(1) + ": " + MASK_PATTERN);
    }

    private String sanitizeConfigDetails(String details) {
        if (!StringUtils.hasText(details)) {
            return details;
        }

        // 설정 정보에서 민감한 정보 마스킹
        return details.replaceAll("(?i)(api[_-]?key|secret|token)=([^,\\s]+)", "$1=" + MASK_PATTERN);
    }

    public void logPromptSummary(String sessionKey, String operation, String promptSummary) {
        log.debug("OpenAI prompt summary - session: {}, operation: {}, summary: {}",
                maskSensitiveData(sessionKey), operation, truncateAndSanitize(promptSummary, 200));
    }

    public void logResponseSummary(String sessionKey, String operation, String responseSummary) {
        log.debug("OpenAI response summary - session: {}, operation: {}, summary: {}",
                maskSensitiveData(sessionKey), operation, truncateAndSanitize(responseSummary, 200));
    }

    private String truncateAndSanitize(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        String sanitized = sanitizeErrorMessage(text);
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }

        return sanitized.substring(0, maxLength - 3) + "...";
    }

    public boolean shouldLogDebug() {
        return log.isDebugEnabled();
    }

    public boolean shouldLogTrace() {
        return log.isTraceEnabled();
    }

    public void logCacheEvent(String event, String key, int size) {
        log.debug("OpenAI cache event - event: {}, key: {}, size: {}",
                event, maskSensitiveData(key), size);
    }
}