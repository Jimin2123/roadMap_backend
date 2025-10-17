package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 4. 관련 학과
@Schema(description = "관련 학과")
public record DepartmentRecord(
        @JsonProperty("depart_id") @Schema(description = "관련학과 ID") Long departId,
        @JsonProperty("depart_name") @Schema(description = "관련학과명") String departName
) {}