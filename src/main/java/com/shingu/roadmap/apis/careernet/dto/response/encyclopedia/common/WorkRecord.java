package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 2. 하는 일
@Schema(description = "하는 일")
public record WorkRecord(
        @JsonProperty("work") @Schema(description = "하는 일 내용") String work
) {}