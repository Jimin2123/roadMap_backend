package com.shingu.roadmap.apis.openai.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OpenAiInputValidator {

    private static final int MAX_PROMPT_LENGTH = 100000;
    private static final int MAX_SESSION_KEY_LENGTH = 200;
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\n\\r\\t]*$");
    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9:_-]+$");

    private static final List<String> SUSPICIOUS_PATTERNS = Arrays.asList(
        "(?i)<script[^>]*>.*?</script>",
        "(?i)javascript:",
        "(?i)data:text/html",
        "(?i)vbscript:",
        "(?i)onload\\s*=",
        "(?i)onclick\\s*=",
        "(?i)onerror\\s*=",
        "(?i)select.*from.*where",
        "(?i)drop\\s+table",
        "(?i)union\\s+select",
        "(?i)insert\\s+into",
        "(?i)delete\\s+from",
        "(?i)update.*set",
        "(?i)exec\\s*\\(",
        "(?i)eval\\s*\\(",
        "(?i)system\\s*\\(",
        "(?i)\\|\\s*nc\\s",
        "(?i)\\|\\s*netcat",
        "(?i)\\|\\s*wget",
        "(?i)\\|\\s*curl"
    );

    public void validateUserInput(String input) {
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("Input cannot be empty or null");
        }

        if (input.length() > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Input exceeds maximum length of %d characters", MAX_PROMPT_LENGTH));
        }

        if (!SAFE_TEXT_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("Input contains invalid characters");
        }

        if (containsSuspiciousContent(input)) {
            log.warn("Suspicious content detected in input: {}",
                     input.length() > 100 ? input.substring(0, 100) + "..." : input);
            throw new SecurityException("Input contains potentially harmful content");
        }

        log.debug("Input validation passed for {} characters", input.length());
    }

    public void validateSessionKey(String sessionKey) {
        if (!StringUtils.hasText(sessionKey)) {
            throw new IllegalArgumentException("Session key cannot be empty or null");
        }

        if (sessionKey.length() > MAX_SESSION_KEY_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Session key exceeds maximum length of %d characters", MAX_SESSION_KEY_LENGTH));
        }

        if (!SESSION_KEY_PATTERN.matcher(sessionKey).matches()) {
            throw new IllegalArgumentException("Session key contains invalid characters");
        }

        log.debug("Session key validation passed: {}", sessionKey);
    }

    public void validatePromptLength(String systemPrompt, String userPrompt) {
        int totalLength = (systemPrompt != null ? systemPrompt.length() : 0) +
                         (userPrompt != null ? userPrompt.length() : 0);

        if (totalLength > MAX_PROMPT_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Combined prompt length (%d) exceeds maximum (%d)",
                             totalLength, MAX_PROMPT_LENGTH));
        }
    }

    public String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        // HTML 특수문자 이스케이프
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }

    private boolean containsSuspiciousContent(String input) {
        String lowercaseInput = input.toLowerCase();

        return SUSPICIOUS_PATTERNS.stream()
                .anyMatch(pattern -> Pattern.compile(pattern).matcher(lowercaseInput).find());
    }

    public boolean isInputSafe(String input) {
        try {
            validateUserInput(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public InputValidationResult validateWithDetails(String input) {
        try {
            validateUserInput(input);
            return new InputValidationResult(true, "Valid input", null);
        } catch (Exception e) {
            return new InputValidationResult(false, e.getMessage(), e.getClass().getSimpleName());
        }
    }

    public static class InputValidationResult {
        private final boolean valid;
        private final String message;
        private final String errorType;

        public InputValidationResult(boolean valid, String message, String errorType) {
            this.valid = valid;
            this.message = message;
            this.errorType = errorType;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getErrorType() { return errorType; }
    }
}