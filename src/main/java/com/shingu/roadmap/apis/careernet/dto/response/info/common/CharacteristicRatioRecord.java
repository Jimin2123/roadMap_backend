package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "특성별 비율 정보 (성별/학교급)")
public record CharacteristicRatioRecord(
        @JsonProperty("popular") @Schema(description = "많이 본 직업 기준") RatioDetailRecord popular,
        @JsonProperty("bookmark") @Schema(description = "관심 직업 기준") RatioDetailRecord bookmark
) {}