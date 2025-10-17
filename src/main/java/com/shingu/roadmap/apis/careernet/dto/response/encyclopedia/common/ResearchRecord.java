package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 9. 진로탐색
@Schema(description = "진로탐색")
public record ResearchRecord(
        @JsonProperty("research") @Schema(description = "진로탐색활동") String research
) {}