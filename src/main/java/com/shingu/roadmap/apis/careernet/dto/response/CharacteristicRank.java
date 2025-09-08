package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "특성 순위 항목 DTO (적성/가치관)")
@Getter
@Setter
public class CharacteristicRank {
  @Schema(description = "순위", example = "1")
  private String rank;

  @Schema(description = "정렬 순서", example = "1")
  private String cdOrdr;

  @Schema(description = "선호명", example = "성취")
  private String cdNm;
}