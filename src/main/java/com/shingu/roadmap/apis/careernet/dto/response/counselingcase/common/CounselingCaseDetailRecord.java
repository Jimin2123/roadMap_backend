package com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "진로 상담 사례 상세 정보")
public record CounselingCaseDetailRecord(
        @JsonProperty("gubun")
        @Schema(description = "상담사례 분류코드")
        String gubun,

        @JsonProperty("question")
        @Schema(description = "상담사례 질문")
        String question,

        @JsonProperty("answer")
        @Schema(description = "상담사례 답변")
        String answer
) {}