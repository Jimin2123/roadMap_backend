package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지식중요도 상세")
public record KnowledgeDetailRecord(
        @JsonProperty("knowledge") String knowledge,
        @JsonProperty("inform") String inform,
        @JsonProperty("importance") Integer importance,
        @JsonProperty("source") String source
) {}