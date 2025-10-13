package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 또는 분석의 근거 정보")
public record Evidence(
        @Schema(description = "근거의 출처 (예: 이력서, 자기소개서, 진단 결과 등)", example = "이력서")
        String source,

        @Schema(description = "근거가 되는 구체적인 내용 또는 텍스트", example = "Spring Boot 경험")
        String content,

        @Schema(description = "해당 근거가 왜 중요한지에 대한 설명", example = "핵심 기술 키워드 일치")
        String reasoning
) {
}