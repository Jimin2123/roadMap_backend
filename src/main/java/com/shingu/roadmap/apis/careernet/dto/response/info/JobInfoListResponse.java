package com.shingu.roadmap.apis.careernet.dto.response.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.info.common.JobInfoRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "직업정보 목록 API 전체 응답 DTO")
public record JobInfoListResponse(

        @JsonProperty("dataSearch")
        @Schema(description = "데이터 검색 결과")
        ContentWrapper dataSearch
) {
        @Schema(description = "직업 정보 리스트를 포함하는 content DTO")
        public record ContentWrapper(
                @JsonProperty("content")
                @Schema(description = "검색 결과 리스트")
                List<JobInfoRecord> content
        ) {}
}