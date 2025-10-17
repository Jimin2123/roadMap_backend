package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "취업 현황")
public record StateOfEmploymentRecord(
        @JsonProperty("empway") @Schema(description = "입직 및 취업방법") String empWay,
        @JsonProperty("employment") @Schema(description = "고용현황") String employment,
        @JsonProperty("salery") @Schema(description = "임금수준") String salary
) {}