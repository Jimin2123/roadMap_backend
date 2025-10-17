package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "직업 전망")
public record JobPossibilityRecord(
        @JsonProperty("possibility") @Schema(description = "전망") String possibility,
        @JsonProperty("chart_item_list") @Schema(description = "차트 데이터 목록") List<ChartItemRecord> chartItemList
) {}