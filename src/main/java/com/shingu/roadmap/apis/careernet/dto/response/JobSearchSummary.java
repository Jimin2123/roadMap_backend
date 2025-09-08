package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "직업 '검색' 결과 요약 정보 DTO")
@Getter
@Setter
public class JobSearchSummary {

  @Schema(description = "직업명", example = "응용소프트웨어개발자")
  private String job;

  @Schema(description = "직업 코드 ID", example = "1341")
  private String jobdicSeq;

  @Schema(description = "직업 분야", example = "IT·인터넷")
  private String profession;

  @Schema(description = "유사 직업", example = "시스템소프트웨어개발자")
  private String similarJob;

  @Schema(description = "직업 설명 요약", example = "애플리케이션을 설계하고 개발합니다.")
  private String summary;

  @Schema(description = "고용 평등", example = "매우 좋음")
  private String equalemployment;

  @Schema(description = "발전 가능성", example = "높음")
  private String possibility;

  @Schema(description = "일자리 전망", example = "증가 추세")
  private String prospect;

  @Schema(description = "연봉", example = "5000만원 이상")
  private String salery;

  @Schema(description = "직업 코드", example = "4111")
  private String jobCod;

  @Schema(description = "직업 분류 코드", example = "12345")
  private String jobCtgCode;

  @Schema(description = "적성 유형별 코드", example = "S")
  private String aptdTypeCode;
}