package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 8. 관련 태그
@Schema(description = "관련 태그")
public record TagRecord(
        @JsonProperty("tag") @Schema(description = "관련 태그") String tag
) {}