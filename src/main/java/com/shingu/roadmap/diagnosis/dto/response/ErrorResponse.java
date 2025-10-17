package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * API 오류 응답 DTO
 * 클라이언트에게 표준화된 에러 정보를 제공합니다.
 */
@Builder
@Schema(description = "API 오류 응답")
public record ErrorResponse(
        @Schema(description = "오류 코드", example = "DIAGNOSIS_NOT_FOUND")
        String errorCode,

        @Schema(description = "오류 메시지", example = "진단 결과를 찾을 수 없습니다.")
        String message,

        @Schema(description = "오류 발생 시각", example = "2025-01-15T10:30:00")
        LocalDateTime timestamp,

        @Schema(description = "요청 경로", example = "/api/v1/diagnosis/result/123")
        String path,

        @Schema(description = "추적 ID (디버깅용)", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
        String traceId,

        @Schema(description = "유효성 검증 오류 상세 정보 (validation 에러인 경우)")
        List<ValidationErrorDetail> validationErrors
) {
    /**
     * 기본 에러 응답 생성
     */
    public static ErrorResponse of(String errorCode, String message, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .traceId(generateTraceId())
                .build();
    }

    /**
     * Validation 에러 응답 생성
     */
    public static ErrorResponse ofValidation(String errorCode, String message, String path, List<ValidationErrorDetail> validationErrors) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .traceId(generateTraceId())
                .validationErrors(validationErrors)
                .build();
    }

    /**
     * 추적 ID 생성 (디버깅 및 로그 상관관계를 위한 UUID)
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
