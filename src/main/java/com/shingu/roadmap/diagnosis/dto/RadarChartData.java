package com.shingu.roadmap.diagnosis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "레이더 차트 데이터 (사용자 역량 vs 여러 목표 NCS 직무 동시 비교)")
public record RadarChartData(
        @Schema(description = "사용자의 현재 역량 프로필")
        CompetencyProfile userProfile,

        @Schema(description = "비교 대상 NCS 직무 역량 프로필 목록")
        List<NcsCompetencyProfile> targetNcsProfiles,

        @Schema(description = "레이더 차트의 축으로 사용될 공통 역량 목록", example = "[\"프로그래밍 언어 활용\", \"데이터베이스 구축\", \"요구사항 분석\"]")
        List<String> competencyAxes
) {
}