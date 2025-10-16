package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직업 요약 정보")
public record JobSummaryRecord(

        @JsonProperty("aptit_name")
        @Schema(description = "직업군")
        String aptitName,

        @JsonProperty("work")
        @Schema(description = "하는 일")
        String work,

        @JsonProperty("job_cd")
        @Schema(description = "직업코드")
        String jobCd,

        @JsonProperty("rel_job_nm")
        @Schema(description = "관련직업")
        String relJobNm,

        @JsonProperty("job_nm")
        @Schema(description = "직업명")
        String jobNm,

        @JsonProperty("wlb")
        @Schema(description = "일과 삶의 균형 수준")
        String wlb,

        @JsonProperty("edit_dt")
        @Schema(description = "수정일 (타임스탬프)")
        Long editDt,

        @JsonProperty("reg_dt")
        @Schema(description = "등록일 (타임스탬프)")
        Long regDt,

        @JsonProperty("views")
        @Schema(description = "조회수")
        Integer views,

        @JsonProperty("likes")
        @Schema(description = "추천수")
        Integer likes,

        @JsonProperty("wage")
        @Schema(description = "연봉수준")
        String wage
) {}