package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "특성별 선호도 정보 (가치/적성)")
public record CharacteristicPreferenceRecord(
        @JsonProperty("popular") @Schema(description = "많이 본 직업 기준") List<PreferenceDetailRecord> popular,
        @JsonProperty("bookmark") @Schema(description = "관심 직업 기준") List<PreferenceDetailRecord> bookmark
) {}