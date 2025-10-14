package com.shingu.roadmap.diagnosis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

@Builder(toBuilder = true)
@Schema(description = "NCS 직무의 요구 역량 프로필")
public record NcsCompetencyProfile(
        @Schema(
                description = "NCS 코드",
                example = "02010201",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String ncsCode,

        @Schema(
                description = "NCS 직무명",
                example = "응용 SW 엔지니어링",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String ncsName,

        @Schema(
                description = "해당 직무의 역량명과 요구 점수 매핑 (key: 역량명, value: 요구 점수 0.0~1.0)",
                example = "{\"프로그래밍 언어 활용\": 0.85, \"데이터베이스 구축\": 0.80}",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Map<String, Double> competencyScores,

        @Schema(
                description = "사용자와의 전체 일치율 (0.0 ~ 1.0)",
                example = "0.72",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Double matchRate
) {
}
