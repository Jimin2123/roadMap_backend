package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "관련 학과 정보 DTO")
@Getter
@Setter
public class Major {

  @Schema(description = "ROWNUM", example = "1")
  private String rnum;

  @Schema(description = "학과 SEQ", example = "1028")
  private String majorSeq;

  @Schema(description = "학과명", example = "컴퓨터공학과")
  private String majorNm;
}