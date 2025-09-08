package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "특성 데이터 DTO (많이 본/관심 직업 포함)")
@Getter
@Setter
public class CharacteristicData<T> {

  @Schema(description = "많이 본 통계")
  private List<T> popular;

  @Schema(description = "관심 직업 통계")
  private List<T> bookmark;
}