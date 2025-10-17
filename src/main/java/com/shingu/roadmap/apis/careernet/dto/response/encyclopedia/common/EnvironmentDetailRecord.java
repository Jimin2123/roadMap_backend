package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업무환경 상세")
public record EnvironmentDetailRecord(
        @JsonProperty("environment") String environment,
        @JsonProperty("inform") String inform,
        @JsonProperty("importance") Integer importance,
        @JsonProperty("source") String source
) {}