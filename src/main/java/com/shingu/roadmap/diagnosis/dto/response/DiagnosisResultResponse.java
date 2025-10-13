package com.shingu.roadmap.diagnosis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최종 역량 진단 결과 응답 DTO")
public record DiagnosisResultResponse(
        @Schema(description = "진단 ID", example = "1")
        Long diagnosisId,

        @Schema(description = "진단 결과 요약")
        String summary,

        @Schema(description = "추천 NCS 코드 목록")
        List<NcsAnalysisResponse> ncsAnalyses,

        @Schema(description = "추천 직무 목록")
        List<String> recommendedJobs,

        @Schema(description = "추천 교육 목록")
        List<String> recommendedTrainings,

        @Schema(description = "KSA 분석 결과")
        KsaAnalysisResponse ksaAnalysis,

        @Schema(description = "전체 분석에 대한 신뢰도 점수 (0.0 ~ 1.0)", example = "0.88")
        Double confidenceScore,

        @Schema(description = "레이더 차트 데이터 (목표 직무 vs 사용자 역량 갭)")
        RadarChartData radarChartData
) {
}