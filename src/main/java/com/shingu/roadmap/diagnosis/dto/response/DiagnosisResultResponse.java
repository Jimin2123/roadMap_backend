package com.shingu.roadmap.diagnosis.dto.response;

import com.shingu.roadmap.diagnosis.dto.RadarChartData;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최종 역량 진단 결과 응답 DTO")
public record DiagnosisResultResponse(
        @Schema(description = "진단 ID", example = "1")
        Long diagnosisId,

        @Schema(description = "진단 결과 요약")
        String summary,

        @Schema(description = "추천 NCS 코드 목록 (각 후보별 KSA 분석 포함)")
        List<NcsAnalysisResponse> ncsAnalyses,

        @Schema(description = "전체 분석에 대한 신뢰도 점수 (0.0 ~ 1.0)", example = "0.88")
        Double confidenceScore,

        @Schema(description = "레이더 차트 데이터 (사용자 역량 vs 여러 목표 NCS 직무 동시 비교)")
        RadarChartData radarChartData
) {
}