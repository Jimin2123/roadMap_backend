package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "특성 통계 항목 DTO (성별/학교급별)")
@Getter
@Setter
public class CharacteristicItem {
  @Schema(description = "비율 (정수)", example = "75")
  private String pcnt1;

  @Schema(description = "비율 (소수)", example = "3")
  private String pcnt2;

  @Schema(description = "비율 (반올림)", example = "75")
  private String pcnt;

  @Schema(description = "항목명 (성별, 학교급 등)", example = "남성")
  private String itemName; // GEN_NM, SCH_CLASS_NM을 공통으로 사용
}