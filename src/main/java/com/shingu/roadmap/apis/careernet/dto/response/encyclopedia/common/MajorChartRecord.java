package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 16. 전공 분포 차트
@Schema(description = "전공 분포 차트")
public record MajorChartRecord(
        @JsonProperty("major") @Schema(description = "전공계열명") String major,
        @JsonProperty("major_data") @Schema(description = "전공계열 데이터") String majorData, // String으로 수정
        @JsonProperty("source") @Schema(description = "전공계열 데이터 출처") String source
) {}