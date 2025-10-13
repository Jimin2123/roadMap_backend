package com.shingu.roadmap.diagnosis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역량 진단 시작 요청 DTO")
public record DiagnosisStartRequest(
        @Schema(description = "사용자 선호도 (선택사항)", example = "데이터 분석 직무 위주로 진단해주세요.")
        String preferences
) {
}