package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의 기관")
public record ContactRecord(
        @JsonProperty("MAPNG_NM") @Schema(description = "문의기관명") String mapngName,
        @JsonProperty("MAPNG_SEQ") @Schema(description = "문의기관SEQ") String mapngSeq,
        @JsonProperty("MAPNG_URL") @Schema(description = "관련URL") String mapngUrl
) {}