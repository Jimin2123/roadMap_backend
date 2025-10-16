package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "관련학과 및 자격 정보")
public record CapacityMajorRecord(
        @JsonProperty("capacity") @Schema(description = "관련자격증") String capacity,
        @JsonProperty("major") @Schema(description = "관련학과 목록") List<MajorRecord> majors
) {}