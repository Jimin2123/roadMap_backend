package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "에러 응답 DTO")
@Getter
@Setter
public class ErrorResult {

  @Schema(description = "결과 코드 (0: 성공, 음수: 실패)", example = "-1")
  private String code;

  @Schema(description = "처리 결과 메시지", example = "인증키가 유효하지 않습니다.")
  private String message;
}