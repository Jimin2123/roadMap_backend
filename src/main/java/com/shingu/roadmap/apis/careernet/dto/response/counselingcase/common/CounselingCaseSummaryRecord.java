package com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "진로 상담 사례 요약 정보")
public record CounselingCaseSummaryRecord(
        @JsonProperty("gubun")
        @Schema(description = "상담사례 분류코드")
        String gubun,

        @JsonProperty("memo")
        @Schema(description = "상담사례 질문")
        String memo,

        @JsonProperty("code")
        @Schema(description = "질문코드값")
        String code
) {}