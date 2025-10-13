package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개별 역량 점수 정보 (레이더 차트 축 데이터)")
public record CompetencyScore(
        @Schema(description = "역량명 (NCS 능력단위명 또는 KSA 항목명)", example = "프로그래밍 언어 활용")
        String competencyName,

        @Schema(description = "사용자의 현재 역량 점수 (0.0 ~ 1.0)", example = "0.65")
        Double userScore,

        @Schema(description = "목표 직무의 요구 역량 점수 (0.0 ~ 1.0)", example = "0.85")
        Double targetScore,

        @Schema(description = "역량 갭 (targetScore - userScore)", example = "0.20")
        Double gap,

        @Schema(description = "역량 수준 평가", example = "ADEQUATE")
        String level
) {
}