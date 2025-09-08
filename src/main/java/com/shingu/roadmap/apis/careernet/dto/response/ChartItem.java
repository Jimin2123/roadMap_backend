package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "직업 전망 차트 항목 DTO")
@Getter
@Setter
public class ChartItem {

  @Schema(description = "항목명", example = "일자리 수")
  private String chartKey;

  @Schema(description = "값", example = "85")
  private String chartValue;
}