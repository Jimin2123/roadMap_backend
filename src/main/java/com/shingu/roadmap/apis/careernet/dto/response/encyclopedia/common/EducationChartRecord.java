package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 15. 학력 분포 차트
@Schema(description = "학력 분포 차트")
public record EducationChartRecord(
        @JsonProperty("chart_data") @Schema(description = "학력분포 데이터") Integer chartData,
        @JsonProperty("source") @Schema(description = "학력분포 출처") String source
) {}