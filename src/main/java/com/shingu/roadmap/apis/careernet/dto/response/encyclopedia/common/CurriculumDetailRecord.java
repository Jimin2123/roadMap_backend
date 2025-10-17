package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정규교육과정 상세")
public record CurriculumDetailRecord(
        @JsonProperty("curriculum") String curriculum
) {}