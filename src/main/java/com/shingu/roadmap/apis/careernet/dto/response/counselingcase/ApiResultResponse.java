package com.shingu.roadmap.apis.careernet.dto.response.counselingcase;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common.ResultRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 처리 결과 응답 DTO")
public record ApiResultResponse(
        @JsonProperty("result")
        @Schema(description = "결과 래퍼 객체")
        ResultRecord result
) {}
