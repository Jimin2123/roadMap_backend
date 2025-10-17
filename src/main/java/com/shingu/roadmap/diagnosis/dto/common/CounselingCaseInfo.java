package com.shingu.roadmap.diagnosis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 진로 상담 사례 정보 (진단 결과용)
 */
@Builder(toBuilder = true)
@Schema(description = "진로 상담 사례 정보")
public record CounselingCaseInfo(
        @Schema(
                description = "상담 사례 제목/요약",
                example = "소프트웨어 개발자가 되려면 어떤 준비가 필요한가요?",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title,

        @Schema(
                description = "상담 질문",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String question,

        @Schema(
                description = "상담 답변",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String answer,

        @Schema(
                description = "상담 분류",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String category
) {
}
