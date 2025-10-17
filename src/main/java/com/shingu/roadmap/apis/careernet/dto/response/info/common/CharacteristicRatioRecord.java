package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "특성별 비율 정보 (성별/학교급)")
public record CharacteristicRatioRecord(
        @JsonProperty("popular") @Schema(description = "많이 본 직업 기준") List<RatioDetailRecord> popular,
        @JsonProperty("bookmark") @Schema(description = "관심 직업 기준") List<RatioDetailRecord> bookmark
) {}