package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분류 정보")
public record DivisionRecord(
        @JsonProperty("cnet_job_dvs") @Schema(description = "커리어넷직업분류") String cnetJobDvs,
        @JsonProperty("std_code_nm") @Schema(description = "표준직업분류") String stdCodeName,
        @JsonProperty("emplym_code_nm") @Schema(description = "고용직업분류") String emplymCodeName
) {}