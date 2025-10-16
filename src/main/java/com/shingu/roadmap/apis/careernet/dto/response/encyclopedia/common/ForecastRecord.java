package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 14. 직업 전망
@Schema(description = "직업 전망")
public record ForecastRecord(
        @JsonProperty("forecast") @Schema(description = "직업전망") String forecast,
        @JsonProperty("chart_name") @Schema(description = "학력분포") String chartName
) {}