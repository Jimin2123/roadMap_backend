package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선호도 상세 정보")
public record PreferenceDetailRecord(
        @JsonProperty("RANK") @Schema(description = "우선순위") String rank,
        @JsonProperty("CD_ORDR") @Schema(description = "정렬순서") String codeOrder,
        @JsonProperty("CD_NM") @Schema(description = "선호명") String codeName
) {}
