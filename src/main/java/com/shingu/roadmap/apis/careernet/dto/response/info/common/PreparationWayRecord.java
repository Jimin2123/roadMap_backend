package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "준비 방법")
public record PreparationWayRecord(
        @JsonProperty("preparation") @Schema(description = "정규교육과정") String preparation,
        @JsonProperty("training") @Schema(description = "직업훈련") String training,
        @JsonProperty("certification") @Schema(description = "관련자격증") String certification
) {}
