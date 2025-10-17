package com.shingu.roadmap.apis.careernet.dto.response.counselingcase.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결과 래퍼")
public record ResultRecord(
        @JsonProperty("content")
        @Schema(description = "결과 내용")
        ResultContentRecord content
) {}