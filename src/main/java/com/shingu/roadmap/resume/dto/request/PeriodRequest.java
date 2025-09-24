package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "기간 요청 DTO")
public record PeriodRequest(
        @Schema(description = "시작일", example = "2023-01-01")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2023-06-01")
        LocalDate endDate
) { }