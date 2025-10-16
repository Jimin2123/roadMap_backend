package com.shingu.roadmap.apis.careernet.dto.response.encyclopedia.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 1. 기본 정보
@Schema(description = "직업 기본 정보")
public record BaseInfoRecord(
        @JsonProperty("aptit_name") @Schema(description = "직업분류") String aptitName,
        @JsonProperty("INTRST_JOB_YN") @Schema(description = "관심직업 설정여부") String intrstJobYn,
        @JsonProperty("emp_job_nm") @Schema(description = "고용코드명") String empJobNm,
        @JsonProperty("social") @Schema(description = "사회공헌") String social,
        @JsonProperty("emp_job_cd") @Schema(description = "고용코드") Integer empJobCd,
        @JsonProperty("job_cd") @Schema(description = "직업코드") Integer jobCd,
        @JsonProperty("satisfication") @Schema(description = "직업만족도") Integer satisfication,
        @JsonProperty("rel_job_nm") @Schema(description = "관련직업명") String relJobNm,
        @JsonProperty("job_nm") @Schema(description = "직업명") String jobNm,
        @JsonProperty("std_job_nm") @Schema(description = "표준직업코드명") String stdJobNm,
        @JsonProperty("wlb") @Schema(description = "일가정균형") String wlb,
        @JsonProperty("std_job_cd") @Schema(description = "표준직업코드") String stdJobCd,
        @JsonProperty("wage_source") @Schema(description = "평균연봉 출처") String wageSource,
        @JsonProperty("edit_dt") @Schema(description = "수정일") String editDt,
        @JsonProperty("reg_dt") @Schema(description = "작성일") String regDt,
        @JsonProperty("satisfi_source") @Schema(description = "직업만족도 출처") String satisfiSource,
        @JsonProperty("tag") @Schema(description = "태그") String tag,
        @JsonProperty("views") @Schema(description = "조회수") Integer views,
        @JsonProperty("likes") @Schema(description = "추천수") Integer likes,
        @JsonProperty("wage") @Schema(description = "평균연봉") Integer wage
) {}
