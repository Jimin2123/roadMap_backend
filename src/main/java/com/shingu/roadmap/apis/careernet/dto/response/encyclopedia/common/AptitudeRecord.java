package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 6. 적성
@Schema(description = "적성")
public record AptitudeRecord(
        @JsonProperty("aptitude") @Schema(description = "적성") String aptitude
) {}