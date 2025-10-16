package com.shingu.roadmap.apis.careernet.dto.response.counselingcase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common.CounselingCaseSummaryRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "진로 상담 사례 목록 응답 DTO")
public record CounselingCaseListResponse(
        @JsonProperty("totalCount")
        @Schema(description = "전체 검색 결과 수")
        String totalCount,

        @JsonProperty("content")
        @Schema(description = "전체 검색 결과 리스트")
        List<CounselingCaseSummaryRecord> content
) {}