package com.shingu.roadmap.apis.careernet.dto.response.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.info.common.ContentRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업정보 상세 응답 DTO")
public record JobInfoDetailResponse(
        @JsonProperty("content")
        @Schema(description = "전체 검색 결과 및 상세 정보")
        ContentRecord content
) {}