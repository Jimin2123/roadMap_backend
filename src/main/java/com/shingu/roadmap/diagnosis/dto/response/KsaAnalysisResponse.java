package com.shingu.roadmap.diagnosis.dto.response;

import com.shingu.roadmap.diagnosis.dto.common.Evidence;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "KSA (Knowledge, Skill, Attitude) 역량 세부 분석 결과")
public record KsaAnalysisResponse(
        @Schema(
                description = "분석의 기준이 된 NCS 코드",
                example = "02010201",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String ncsCode,

        @Schema(
                description = "지식(Knowledge) 관련 분석 항목",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<KsaItem> knowledgeItems,

        @Schema(
                description = "기술(Skill) 관련 분석 항목",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<KsaItem> skillItems,

        @Schema(
                description = "태도(Attitude) 관련 분석 항목",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<KsaItem> attitudeItems,

        @Schema(
                description = "KSA 분석에 대한 전체적인 평가 요약",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String overallAssessment,

        @Schema(
                description = "분석 근거 목록 (원본 텍스트에서 추출된 구체적인 증거)",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<Evidence> evidenceList
) {
    @Builder(toBuilder = true)
    @Schema(description = "KSA 개별 항목 분석 결과")
    public record KsaItem(
            @Schema(
                    description = "항목명",
                    example = "프로그래밍 언어 활용",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String itemName,

            @Schema(
                    description = "항목 상세 설명",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String itemDescription,

            @Schema(
                    description = "사용자 보유 수준 점수 (0.0 ~ 1.0)",
                    example = "0.75",
                    minimum = "0.0",
                    maximum = "1.0",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Double userScore,

            @Schema(
                    description = "목표 수준 점수 (0.0 ~ 1.0)",
                    example = "0.85",
                    minimum = "0.0",
                    maximum = "1.0",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Double targetScore,

            @Schema(
                    description = "점수 갭 (targetScore - userScore)",
                    example = "0.10",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Double scoreGap,

            @Schema(
                    description = "수준 평가 (정성적)",
                    example = "ADEQUATE",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String levelAssessment,

            @Schema(
                    description = "목표 수준 대비 부족한 부분 (정성적 설명)",
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            String gapDescription,

            @Schema(
                    description = "역량 강화를 위한 추천 방안",
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            String recommendation
    ) {
    }
}
