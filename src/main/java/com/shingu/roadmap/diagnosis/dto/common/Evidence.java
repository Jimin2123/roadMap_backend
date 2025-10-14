package com.shingu.roadmap.diagnosis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@Schema(description = "추천 또는 분석의 근거 정보")
public record Evidence(
        @Schema(
                description = "근거의 출처 타입",
                example = "RESUME",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        EvidenceSourceType sourceType,

        @Schema(
                description = "상세 출처 (섹션/항목명)",
                example = "경력사항 > ABC 회사 백엔드 개발자",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String sourceDetail,

        @Schema(
                description = "근거가 되는 구체적인 내용 또는 텍스트",
                example = "Spring Boot를 활용한 RESTful API 개발 3년 경험",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String content,

        @Schema(
                description = "해당 근거가 왜 중요한지에 대한 설명",
                example = "핵심 기술 키워드 일치 및 실무 경험 보유",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String reasoning
) {
}