package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수행능력 상세 항목")
public record PerformanceDetailRecord(
        @JsonProperty("environment") @Schema(description = "업무환경 능력명", nullable = true) String environment,
        @JsonProperty("perform") @Schema(description = "업무수행능력 능력명", nullable = true) String perform,
        @JsonProperty("knowledge") @Schema(description = "지식중요도 능력명", nullable = true) String knowledge,
        @JsonProperty("inform") @Schema(description = "설명") String inform,
        @JsonProperty("importance") @Schema(description = "중요도") Integer importance,
        @JsonProperty("source") @Schema(description = "출처") String source
) {}