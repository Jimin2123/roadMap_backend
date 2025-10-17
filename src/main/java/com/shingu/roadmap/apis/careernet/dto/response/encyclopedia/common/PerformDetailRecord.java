package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업무수행능력 상세")
public record PerformDetailRecord(
        @JsonProperty("perform") String perform,
        @JsonProperty("inform") String inform,
        @JsonProperty("importance") Integer importance,
        @JsonProperty("source") String source
) {}