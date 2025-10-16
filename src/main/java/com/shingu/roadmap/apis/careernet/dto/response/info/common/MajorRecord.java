package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관련학과 상세")
public record MajorRecord(
        @JsonProperty("TOTAL_CNT") @Schema(description = "관련학과수") String totalCount,
        @JsonProperty("RNUM") @Schema(description = "ROWNUM") String rowNum,
        @JsonProperty("MAJOR_SEQ") @Schema(description = "학과SEQ") String majorSeq,
        @JsonProperty("MAJOR_NM") @Schema(description = "학과명") String majorName
) {}