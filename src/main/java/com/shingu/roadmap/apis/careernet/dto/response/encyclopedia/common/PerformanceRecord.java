package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "수행능력, 지식, 환경")
public record PerformanceRecord(
        @JsonProperty("environment") @Schema(description = "업무환경") List<EnvironmentDetailRecord> environment,
        @JsonProperty("perform") @Schema(description = "업무수행능력") List<PerformDetailRecord> perform,
        @JsonProperty("knowledge") @Schema(description = "지식중요도") List<KnowledgeDetailRecord> knowledge
) {}