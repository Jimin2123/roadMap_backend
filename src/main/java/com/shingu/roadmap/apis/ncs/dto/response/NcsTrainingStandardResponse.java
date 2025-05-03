package com.shingu.roadmap.apis.ncs.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "NCS 훈련기준 고려사항 DTO")
public record NcsTrainingStandardResponse(
        @Schema(description = "훈련기준 리스트")
        List<NcsTrainingStandardItem> data,

        @Schema(description = "응답 정보")
        NcsTrainingStandardDataInfo dataInfo
) {
  @Schema(description = "NCS 훈련기준 고려사항 항목")
  public record NcsTrainingStandardItem(
      @Schema(description = "항목구분코드", example = "01")
      String itemCd,

      @Schema(description = "항목구분명", example = "관련자격종목")
      String itemName,

      @Schema(description = "항목번호", example = "01")
      String itemNo,

      @Schema(description = "항목 세부내용", example = "정보처리기사")
      String defText
  ) {}

  @Schema(description = "NCS 훈련기준 고려사항 응답 정보")
  public record NcsTrainingStandardDataInfo(
      @Schema(description = "응답 코드")
      String code,

      @Schema(description = "응답 메시지")
      String message
  ) {}
}
