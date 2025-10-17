package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 유효성 검증 오류 상세 정보
 */
@Builder
@Schema(description = "유효성 검증 오류 상세 정보")
public record ValidationErrorDetail(
        @Schema(description = "필드명", example = "selectedNcsCode")
        String field,

        @Schema(description = "거부된 값", example = "abc123")
        Object rejectedValue,

        @Schema(description = "오류 메시지", example = "NCS 코드 형식이 올바르지 않습니다.")
        String message
) {
    public static ValidationErrorDetail of(String field, Object rejectedValue, String message) {
        return ValidationErrorDetail.builder()
                .field(field)
                .rejectedValue(rejectedValue)
                .message(message)
                .build();
    }
}
