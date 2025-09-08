package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "문의 기관 정보 DTO")
@Getter
@Setter
public class ContactInstitute {

  @Schema(description = "문의 기관명", example = "한국소프트웨어산업협회")
  private String mapngNm;

  @Schema(description = "문의 기관 SEQ", example = "10011")
  private String mapngSeq;

  @Schema(description = "관련 URL", example = "http://www.sw.or.kr")
  private String mapngUrl;
}