package com.shingu.roadmap.diagnosis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

@Builder(toBuilder = true)
@Schema(description = "역량 프로필 (각 역량별 점수)")
public record CompetencyProfile(
        @Schema(
                description = "프로필 이름",
                example = "사용자 현재 역량",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String profileName,

        @Schema(
                description = "역량명과 점수 매핑 (key: 역량명, value: 점수 0.0~1.0)",
                example = "{\"프로그래밍 언어 활용\": 0.75, \"데이터베이스 구축\": 0.65}",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Map<String, Double> competencyScores
) {
}
