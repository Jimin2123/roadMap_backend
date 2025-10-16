package com.shingu.roadmap.apis.careernet.dto.response.counselingcase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common.CounselingCaseDetailRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "진로 상담 사례 상세 응답 DTO")
public record CounselingCaseDetailResponse(
        @JsonProperty("content")
        @Schema(description = "전체 검색 결과 상세")
        CounselingCaseDetailRecord content
) {}