package com.shingu.roadmap.apis.ncs.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "NCS 직무 상세정보 응답 DTO")
public record NcsJobPositionResponse(
        @Schema(description = "직무 데이터")
        JobData data,

        @Schema(description = "응답 정보")
        ResponseInfo dataInfo
) {

  @Schema(description = "직무 데이터 상세")
  public record JobData(
          @Schema(description = "직책 정보 리스트")
          List<ClposDataItem> clposData,

          @Schema(description = "직책별 능력단위 리스트")
          List<ClposCompeDataItem> clposCompeData
  ) {}

  @Schema(description = "직책 정보 항목")
  public record ClposDataItem(
          @Schema(description = "직책번호", example = "PJT20230000202301")
          String clposNo,

          @Schema(description = "직책명", example = "노무관리 실무자")
          String clposName,

          @Schema(description = "직책수준", example = "3")
          int clposLevel // String -> int 로 수정
  ) {}

  @Schema(description = "직책별 능력단위 항목")
  public record ClposCompeDataItem(
          @Schema(description = "직책번호", example = "PJT20230000202401")
          String clposNo,

          @Schema(description = "직책명", example = "노무관리 초급관리자")
          String clposName,

          @Schema(description = "직책수준", example = "4")
          int clposLevel, // String -> int 로 수정

          @Schema(description = "능력단위코드", example = "07")
          String ncsCompeUnitCd,

          @Schema(description = "능력단위명", example = "노사협의회 운영")
          String compeUnitName,

          @Schema(description = "능력단위분류번호", example = "0202020207_23v3")
          String ncsClCd
  ) {}

  @Schema(description = "응답 정보")
  public record ResponseInfo(
          @Schema(description = "처리상태에 대한 코드", example = "000")
          String code,

          @Schema(description = "처리상태에 대한 메시지", example = "정상")
          String message
  ) {}
}