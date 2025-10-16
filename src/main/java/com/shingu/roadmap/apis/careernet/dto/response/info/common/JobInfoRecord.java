package com.shingu.roadmap.apis.careernet.dto.response.info.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개별 직업 정보")
public record JobInfoRecord(

        @JsonProperty("job")
        @Schema(description = "직업명")
        String job,

        @JsonProperty("jobdicSeq")
        @Schema(description = "직업 코드 ID")
        String jobdicSeq,

        @JsonProperty("profession")
        @Schema(description = "직업분야")
        String profession,

        @JsonProperty("similarJob")
        @Schema(description = "유사직업")
        String similarJob,

        @JsonProperty("summary")
        @Schema(description = "직업설명")
        String summary,

        @JsonProperty("equalemployment")
        @Schema(description = "고용평등")
        String equalEmployment,

        @JsonProperty("possibility")
        @Schema(description = "발전가능성")
        String possibility,

        @JsonProperty("prospect")
        @Schema(description = "일자리전망")
        String prospect,

        @JsonProperty("salery") // 원본 명세의 오타(salery)를 반영
        @Schema(description = "연봉")
        String salary,

        @JsonProperty("job_code")
        @Schema(description = "직업코드")
        String jobCode,

        @JsonProperty("job_ctg_code")
        @Schema(description = "직업분류코드")
        String jobCtgCode,

        @JsonProperty("aptd_type_code")
        @Schema(description = "적성유형코드")
        String aptdTypeCode
) {}