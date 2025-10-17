package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업훈련 상세")
public record TrainingDetailRecord(
        @JsonProperty("training") String training
) {}