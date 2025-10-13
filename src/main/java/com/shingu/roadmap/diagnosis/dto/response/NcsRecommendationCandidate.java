package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "NCS 코드 추천 후보")
public record NcsRecommendationCandidate(
        @Schema(description = "NCS 코드", example = "02010201")
        String ncsCode,

        @Schema(description = "NCS 직무명", example = "소프트웨어 아키텍트")
        String ncsName,

        @Schema(description = "AI가 판단한 해당 직무와의 적합도 점수 (0.0 ~ 1.0)", example = "0.91")
        Double confidenceScore,

        @Schema(description = "해당 직무를 추천한 이유")
        String reason,

        @Schema(description = "추천 근거 목록 (이력서나 진단 결과에서 추출된 구체적인 증거)")
        List<Evidence> evidenceList
) {
}
