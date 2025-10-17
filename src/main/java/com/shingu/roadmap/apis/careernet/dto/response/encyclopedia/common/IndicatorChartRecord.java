package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 17. 직업 지표 차트
@Schema(description = "직업 지표 차트")
public record IndicatorChartRecord(
        @JsonProperty("indicator") @Schema(description = "직업지표명") String indicator,
        @JsonProperty("indicator_data") @Schema(description = "직업지표 데이터") String indicatorData, // Integer -> String
        @JsonProperty("source") @Schema(description = "직업지표 출처") String source
) {}