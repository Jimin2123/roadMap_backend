package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비율 상세 정보")
public record RatioDetailRecord(
        @JsonProperty("PCNT1") @Schema(description = "비율-정수값") String percent1,
        @JsonProperty("PCNT2") @Schema(description = "비율-소수값") String percent2,
        @JsonProperty("PCNT") @Schema(description = "비율-반올림값") String percent,
        // 필드명이 다르므로 두 개를 모두 정의 (사용되지 않는 필드는 null이 됨)
        @JsonProperty("GEN_NM") @Schema(description = "성별", nullable = true) String genderName,
        @JsonProperty("SCH_CLASS_NM") @Schema(description = "학교급", nullable = true) String schoolClassName
) {}