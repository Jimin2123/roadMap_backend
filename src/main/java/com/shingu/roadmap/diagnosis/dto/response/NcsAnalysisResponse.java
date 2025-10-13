package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "NCS 직무 분석 결과")
public record NcsAnalysisResponse(
        @Schema(description = "추천된 NCS 직무 후보 목록")
        List<NcsRecommendationCandidate> candidates,

        @Schema(description = "AI가 자신의 추천에 대해 갖는 전체적인 신뢰도", example = "0.95")
        Double overallConfidence,

        @Schema(description = "신뢰도가 낮아 사용자의 직접 선택이 필요한지 여부", example = "false")
        Boolean requiresUserSelection,

        @Schema(description = "신뢰도가 높아 자동으로 선택된 NCS 코드", example = "02010201")
        String selectedNcsCode
) {
}
