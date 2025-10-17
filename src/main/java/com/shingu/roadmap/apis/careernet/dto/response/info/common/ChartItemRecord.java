package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차트 아이템")
public record ChartItemRecord(
        @JsonProperty("CHART_KEY") @Schema(description = "항목명") String chartKey,
        @JsonProperty("CHART_VALUE") @Schema(description = "값") String chartValue
) {}