package com.shingu.roadmap.diagnosis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "역량 진단 시작 요청 DTO")
public record DiagnosisStartRequest(
        @Size(max = 500, message = "선호도는 최대 500자까지 입력 가능합니다")
        @Schema(
                description = "사용자 선호도 (선택사항)",
                example = "데이터 분석 직무 위주로 진단해주세요.",
                maxLength = 500,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String preferences
) {
}