package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 7. 흥미
@Schema(description = "흥미")
public record InterestRecord(
        @JsonProperty("interest") @Schema(description = "흥미") String interest
) {}