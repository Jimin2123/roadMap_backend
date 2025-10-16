package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 3. 핵심 능력
@Schema(description = "핵심 능력")
public record AbilityRecord(
        @JsonProperty("ability_name") @Schema(description = "핵심 능력명") String abilityName
) {}
