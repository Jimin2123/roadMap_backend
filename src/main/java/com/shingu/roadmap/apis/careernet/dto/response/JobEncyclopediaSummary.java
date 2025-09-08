package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "직업 상세 정보 DTO")
@Getter
@Setter
@ToString
public class JobEncyclopediaSummary {

  @Schema(description = "직업군", example = "컴퓨터·SW·통신")
  private String aptitName;

  @Schema(description = "하는 일", example = "컴퓨터 시스템의 전반적인 개발 및 감독 업무를 수행합니다.")
  private String work;

  @Schema(description = "직업 코드", example = "1341")
  private String jobCd;

  @Schema(description = "관련 직업", example = "시스템 소프트웨어 개발자, 임베디드 개발자")
  private String relJobNm;

  @Schema(description = "직업명", example = "응용소프트웨어개발자")
  private String jobNm;

  @Schema(description = "WLB 수준", example = "높음")
  private String wlb;

  @Schema(description = "수정일 (Timestamp)", example = "1672531199000")
  private Long editDt;

  @Schema(description = "등록일 (Timestamp)", example = "1640995200000")
  private Long regDt;

  @Schema(description = "조회수", example = "15023")
  private Integer views;

  @Schema(description = "추천수", example = "120")
  private Integer likes;

  @Schema(description = "연봉 수준", example = "5000만원")
  private String wage;
}