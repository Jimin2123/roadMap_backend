package com.shingu.roadmap.resume.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "대외활동 등록 요청 DTO")
public record ActivityRequest(
  @Schema(description = "활동명", example = "고용노동부 공모전 참여")
  @NotBlank(message = "활동명은 필수입니다.")
  @Size(max = 100, message = "활동명은 100자를 초과할 수 없습니다.")
  String title,

  @Schema(description = "소속/기관", example = "고용노동부")
  @Size(max = 100, message = "소속/기관은 100자를 초과할 수 없습니다.")
  String organization,

  @Schema(description = "프로젝트 기간", example = "{\"startDate\":\"2023-01-01\",\"endDate\":\"2023-06-01\"}")
  @Valid
  PeriodRequest period,

  @Schema(description = "활동 내용", example = "고용노동부 주최 공모전에 참여하여 우수상을 수상하였습니다.")
  @Size(max = 1000, message = "활동 내용은 1000자를 초과할 수 없습니다.")
  String description
) { }
