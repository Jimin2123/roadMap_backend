package com.shingu.roadmap.apis.careernet.dto.response.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.info.common.JobInfoRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "직업정보 목록 응답 DTO")
public record JobInfoListResponse(

        @JsonProperty("totalCount")
        @Schema(description = "전체 검색 결과 수")
        String totalCount,

        @JsonProperty("content")
        @Schema(description = "전체 검색 결과 리스트")
        List<JobInfoRecord> content
) {}