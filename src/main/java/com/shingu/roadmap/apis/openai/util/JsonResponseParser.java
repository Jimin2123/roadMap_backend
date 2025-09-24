package com.shingu.roadmap.apis.openai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI API 응답 JSON 파싱을 위한 유틸리티 클래스
 *
 * 주요 기능:
 * - 마크다운 래퍼 제거 (```json, ``` 등)
 * - 안전한 JSON 추출 및 파싱
 * - 타입별 파싱 지원
 * - 파싱 실패 시 적절한 fallback 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonResponseParser {

    private final ObjectMapper objectMapper;

    // JSON 객체나 배열을 추출하는 정규식 패턴들
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}");
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[^\\[\\]]*(?:\\[[^\\[\\]]*\\][^\\[\\]]*)*\\]");

    /**
     * OpenAI 응답에서 Set<String>을 안전하게 파싱
     */
    public Set<String> parseStringSet(String response) {
        try {
            String cleanJson = extractJsonFromResponse(response);
            return objectMapper.readValue(cleanJson, new TypeReference<Set<String>>() {});
        } catch (Exception e) {
            log.error("Set<String> 파싱 실패: {}", response, e);
            return Collections.emptySet();
        }
    }

    /**
     * OpenAI 응답에서 Map<String, String>을 안전하게 파싱
     */
    public Map<String, String> parseStringMap(String response) {
        try {
            String cleanJson = extractJsonFromResponse(response);
            return objectMapper.readValue(cleanJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Map<String, String> 파싱 실패: {}", response, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 일반적인 타입 파싱
     */
    public <T> T parseType(String response, TypeReference<T> typeReference, T defaultValue) {
        try {
            String cleanJson = extractJsonFromResponse(response);
            return objectMapper.readValue(cleanJson, typeReference);
        } catch (Exception e) {
            log.error("타입 {} 파싱 실패: {}", typeReference.getType(), response, e);
            return defaultValue;
        }
    }

    /**
     * OpenAI 응답에서 실제 JSON 부분을 안전하게 추출
     *
     * 처리 순서:
     * 1. 마크다운 래퍼 제거 (```json, ``` 등)
     * 2. 정규식을 이용한 JSON 객체/배열 추출
     * 3. 공백 정리
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("응답이 비어있습니다.");
        }

        String cleaned = response.trim();

        // 1단계: 마크다운 래퍼 제거
        cleaned = removeMarkdownWrappers(cleaned);

        // 2단계: 정규식을 이용한 JSON 추출
        String extracted = extractJsonUsingRegex(cleaned);

        if (extracted == null) {
            // 3단계: fallback - 정리된 문자열 그대로 반환
            log.warn("정규식으로 JSON을 찾을 수 없어서 원본 사용: {}", cleaned);
            return cleaned;
        }

        return extracted.trim();
    }

    /**
     * 마크다운 코드 블록 래퍼 제거
     */
    private String removeMarkdownWrappers(String text) {
        // ```json ... ``` 형태 제거
        text = text.replaceAll("^```(?:json)?\\s*", "");
        text = text.replaceAll("\\s*```$", "");

        // ` ... ` 형태 제거 (단일 백틱)
        if (text.startsWith("`") && text.endsWith("`") && text.length() > 2) {
            text = text.substring(1, text.length() - 1);
        }

        return text.trim();
    }

    /**
     * 정규식을 사용하여 JSON 객체나 배열 추출
     */
    private String extractJsonUsingRegex(String text) {
        // JSON 객체 먼저 시도 {...}
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(text);
        if (objectMatcher.find()) {
            return objectMatcher.group();
        }

        // JSON 배열 시도 [...]
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(text);
        if (arrayMatcher.find()) {
            return arrayMatcher.group();
        }

        return null;
    }

    /**
     * JSON 파싱 가능성 검증
     */
    public boolean isValidJson(String text) {
        try {
            String cleaned = extractJsonFromResponse(text);
            objectMapper.readTree(cleaned);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 디버깅용 - JSON 추출 과정 로그
     */
    public void logJsonExtractionProcess(String originalResponse) {
        if (log.isDebugEnabled()) {
            log.debug("원본 응답: {}", originalResponse);

            try {
                String extracted = extractJsonFromResponse(originalResponse);
                log.debug("추출된 JSON: {}", extracted);
                log.debug("파싱 가능 여부: {}", isValidJson(originalResponse));
            } catch (Exception e) {
                log.debug("JSON 추출 실패: {}", e.getMessage());
            }
        }
    }
}