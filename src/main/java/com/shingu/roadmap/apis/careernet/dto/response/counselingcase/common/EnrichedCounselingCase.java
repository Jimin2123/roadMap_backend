package com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 진로 상담 사례 전체 정보 (요약 + 상세)
 * 리스트 API의 summary 정보와 상세 API의 detail 정보를 결합
 */
@Schema(description = "진로 상담 사례 전체 정보 (요약 + 상세)")
@Builder
public record EnrichedCounselingCase(
        @Schema(description = "상담사례 코드 (con_cd)")
        String code,

        @Schema(description = "상담사례 분류코드")
        String gubun,

        @Schema(description = "상담사례 요약")
        String memo,

        @Schema(description = "상담사례 질문 (상세 - 선택적)")
        String question,

        @Schema(description = "상담사례 답변 (상세 - 선택적)")
        String answer,

        @Schema(description = "상세 조회 성공 여부")
        boolean hasDetailData
) {
    /**
     * Summary 정보만으로 생성 (상세 조회 실패 시)
     */
    public static EnrichedCounselingCase fromSummary(CounselingCaseSummaryRecord summary) {
        return EnrichedCounselingCase.builder()
                .code(summary.code())
                .gubun(summary.gubun())
                .memo(summary.memo())
                .question(null)
                .answer(null)
                .hasDetailData(false)
                .build();
    }

    /**
     * Summary + Detail 정보로 생성
     */
    public static EnrichedCounselingCase fromSummaryAndDetail(
            CounselingCaseSummaryRecord summary,
            CounselingCaseDetailRecord detail) {
        return EnrichedCounselingCase.builder()
                .code(summary.code())
                .gubun(summary.gubun())
                .memo(summary.memo())
                .question(detail.question())
                .answer(detail.answer())
                .hasDetailData(true)
                .build();
    }
}
