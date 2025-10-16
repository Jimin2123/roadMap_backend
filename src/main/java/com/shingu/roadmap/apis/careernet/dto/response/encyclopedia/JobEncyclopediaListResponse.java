package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common.JobSummaryRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "직업백과 목록 응답 DTO")
public record JobEncyclopediaListResponse(

        @JsonProperty("jobs")
        @Schema(description = "직업 정보 목록")
        List<JobSummaryRecord> jobs,

        @JsonProperty("count")
        @Schema(description = "전체 검색 결과 수")
        String count,

        @JsonProperty("pageIndex")
        @Schema(description = "현재 페이지")
        Integer pageIndex,

        @JsonProperty("pageSize")
        @Schema(description = "페이지당 출력 건수")
        Integer pageSize
) {}