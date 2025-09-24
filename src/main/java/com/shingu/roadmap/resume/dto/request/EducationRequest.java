package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "학력 등록 요청 DTO")
public record EducationRequest(
        @Schema(description = "학교명", example = "서울대학교")
        @NotBlank(message = "학교명은 필수입니다.")
        @Size(max = 100, message = "학교명은 100자를 초과할 수 없습니다.")
        String school,

        @Schema(description = "전공", example = "컴퓨터공학")
        @Size(max = 100, message = "전공은 100자를 초과할 수 없습니다.")
        String major,

        @Schema(description = "재학 기간", example = "{\"startDate\":\"2019-03-01\",\"endDate\":\"2023-02-28\"}")
        @Valid
        PeriodRequest period,

        @Schema(description = "학력 상태", example = "졸업")
        @Size(max = 20, message = "학력 상태는 20자를 초과할 수 없습니다.")
        String status
) { }
