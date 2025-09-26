package com.shingu.roadmap.apis.ncs.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "NCS 수행준거 KSA 응답 DTO")
public record NcsKsaResponse(
        @Schema(description = "KSA 항목 리스트")
        List<NcsKsaItem> data,

        @Schema(description = "응답 정보")
        ResponseInfo dataInfo
) {
  @Schema(description = "NCS 수행준거 KSA 항목")
  public record NcsKsaItem(
          @Schema(description = "NCS분류코드", example = "01010101")
          String dutyCd,

          @Schema(description = "직무서비스코드", example = "SVC201400061")
          String dutySvcNo,

          @Schema(description = "NCS분류코드_버전", example = "0202020201_13v1")
          String ncsClCd,

          @Schema(description = "능력단위 코드", example = "01")
          String compUnitCd,

          @Schema(description = "능력단위 명", example = "01.노사관계 계획")
          String compUnitName,

          @Schema(description = "능력단위요소번호", example = "1")
          int compUnitFactrNo,

          @Schema(description = "능력단위요소설명", example = "1.목표 설정하기")
          String compUnitFactrName,

          @Schema(description = "KSA 코드", example = "00")
          String gbnCd,

          @Schema(description = "KSA 명", example = "수행준거")
          String gbnName,

          @Schema(description = "KSA 설명", example = "1.중ㆍ장기 목표를 설정하는데 필요한 노사관계 관련 자료를 수집할 수 있다.")
          String gbnVal,

          @Schema(description = "능력단위 레벨", example = "5")
          int compUnitLevel
  ) {}

  @Schema(description = "응답 정보")
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