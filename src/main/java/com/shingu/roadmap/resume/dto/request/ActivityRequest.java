package com.shingu.roadmap.resume.dto.request;

import com.shingu.roadmap.resume.domain.Period;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대외활동 등록 요청 DTO")
public record ActivityRequest(
  @Schema(description = "활동명", example = "고용노동부 공모전 참여")
  String title,

  @Schema(description = "소속/기관", example = "고용노동부")
  String organization,

  @Schema(description = "프로젝트 기간", example = "{\"startDate\":\"2023-01-01\",\"endDate\":\"2023-06-01\"}")
  PeriodRequest period,

  @Schema(description = "활동 내용", example = "고용노동부 주최 공모전에 참여하여 우수상을 수상하였습니다.")
  String description
) { }
