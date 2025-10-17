package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "준비 방법 상세")
public record JobReadyDetailRecord(
        @JsonProperty("recruit") @Schema(description = "입직 및 취업방법", nullable = true) String recruit,
        @JsonProperty("certificate") @Schema(description = "관련자격증", nullable = true) String certificate,
        @JsonProperty("training") @Schema(description = "직업훈련", nullable = true) String training,
        @JsonProperty("curriculum") @Schema(description = "정규교육과정", nullable = true) String curriculum
) {}