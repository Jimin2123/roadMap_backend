package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "직업 상세 정보 응답 DTO")
@Getter
@Setter
public class JobDetailResponse {

  @Schema(description = "직업 상세 정보")
  private JobDetail content;
}