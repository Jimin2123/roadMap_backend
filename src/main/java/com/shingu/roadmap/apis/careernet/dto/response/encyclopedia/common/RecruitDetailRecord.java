package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입직 및 취업방법 상세")
public record RecruitDetailRecord(
        @JsonProperty("recruit") String recruit
) {}