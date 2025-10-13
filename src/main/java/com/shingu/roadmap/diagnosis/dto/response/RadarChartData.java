package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "레이더 차트 데이터 (목표 직무 vs 사용자 역량 비교)")
public record RadarChartData(
        @Schema(description = "목표 NCS 코드", example = "02010201")
        String targetNcsCode,

        @Schema(description = "목표 NCS 직무명", example = "응용 SW 엔지니어링")
        String targetNcsName,

        @Schema(description = "각 역량별 점수 목록 (레이더 차트의 각 축)")
        List<CompetencyScore> competencyScores,

        @Schema(description = "전체 역량 일치율 (0.0 ~ 1.0)", example = "0.72")
        Double overallMatchRate,

        @Schema(description = "평균 갭 크기", example = "0.18")
        Double averageGap
) {
}