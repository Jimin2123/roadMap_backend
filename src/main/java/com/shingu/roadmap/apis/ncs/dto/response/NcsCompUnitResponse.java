package com.shingu.roadmap.apis.ncs.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "NCS 능력단위 응답 DTO")
public record NcsCompUnitResponse(
        @Schema(description = "능력단위 리스트")
        List<NcsCompUnitItem> data,

        @Schema(description = "응답 정보")
        ResponseInfo dataInfo
) {
  @Schema(description = "NCS 능력단위 항목")
  public record NcsCompUnitItem(
          @Schema(description = "NCS분류코드", example = "01010101")
          String dutyCd,

          @Schema(description = "직무서비스코드", example = "SVC201600263")
          String dutySvcNo,

          @Schema(description = "NCS분류코드_버전", example = "0101010101_15v1")
          String ncsClCd,

          @Schema(description = "능력단위 코드", example = "01")
          String compUnitCd,

          @Schema(description = "능력단위 명", example = "01.공적개발원조사업 개발전략수립")
          String compUnitName,

          @Schema(description = "능력단위 설명", example = "공적개발원조사업 개발전략 수립이란...")
          String compUnitDef,

          @Schema(description = "능력단위 레벨", example = "7")
          int compUnitLevel
  ) {}

  @Schema(description = "NCS 능력단위 응답 정보")
  public record ResponseInfo(
          @Schema(description = "처리상태에 대한 코드", example = "000")
          String code,

          @Schema(description = "처리상태에 대한 메시지", example = "정상")
          String message,

          @Schema(description = "전체 페이지 수", example = "1")
          int totalPage,

          @Schema(description = "현재 페이지 번호", example = "1")
          String pageNo,

          @Schema(description = "전체 건수", example = "100")
          int totCnt
  ) {}
}