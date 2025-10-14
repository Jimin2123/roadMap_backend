package com.shingu.roadmap.diagnosis.dto.common;

import com.shingu.roadmap.diagnosis.dto.response.KsaAnalysisResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "NCS 코드 추천 후보")
public record NcsRecommendationCandidate(
        @Schema(
                description = "NCS 코드",
                example = "02010201",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String ncsCode,

        @Schema(
                description = "NCS 직무명",
                example = "소프트웨어 아키텍트",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String ncsName,

        @Schema(
                description = "AI가 판단한 해당 직무와의 적합도 점수 (0.0 ~ 1.0)",
                example = "0.91",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Double confidenceScore,

        @Schema(
                description = "해당 직무를 추천한 이유",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String reason,

        @Schema(
                description = "추천 근거 목록 (이력서나 진단 결과에서 추출된 구체적인 증거)",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<Evidence> evidenceList,

        @Schema(
                description = "해당 NCS 직무에 대한 KSA 역량 분석 결과 (선택적)",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        KsaAnalysisResponse ksaAnalysis,

        @Schema(
                description = "커리어넷 직업 정보 (직업 상세 정보 보강용)",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        CareerNetJobInfo careerNetJobInfo
) {
}
