package com.shingu.roadmap.apis.careernet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "직업백과 API 요청 DTO")
@Getter
@Setter
@ToString
public class JobEncyclopediaRequest {

  @Schema(description = "페이지 번호", defaultValue = "1", example = "1")
  private Integer pageIndex = 1;

  @Schema(description = "검색어 (직업명)", example = "개발자")
  private String searchJobNm;

  @Schema(description = "직업 테마 코드. (예: 102428)", example = "102428")
  private String searchThemeCode;

  @Schema(description = "직업 적성유형 코드. (예: 104740)", example = "104740")
  private String searchAptdCodes;

  @Schema(description = "직업 분류 코드. (예: 1)", example = "1")
  private String searchJobCd;
}