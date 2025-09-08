package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학력 등록 요청 DTO")
public record EducationRequest(
        @Schema(description = "학교명", example = "서울대학교")
        String school,

        @Schema(description = "전공", example = "컴퓨터공학")
        String major,

        @Schema(description = "재학 기간", example = "{\"startDate\":\"2019-03-01\",\"endDate\":\"2023-02-28\"}")
        PeriodRequest period,

        @Schema(description = "학력 상태", example = "졸업")
        String status
) { }
