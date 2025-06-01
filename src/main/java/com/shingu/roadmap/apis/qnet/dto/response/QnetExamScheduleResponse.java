package com.shingu.roadmap.apis.qnet.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record QnetExamScheduleResponse(

        @Schema(description = "응답 헤더")
        Header header,

        @Schema(description = "응답 바디")
        Body body

) {
  public record Header(
          @Schema(description = "응답 결과 코드")
          String resultCode,

          @Schema(description = "응답 메시지")
          String resultMsg
  ) {}

  public record Body(

          @Schema(description = "시험 일정 목록")
          @JsonProperty("items")
          java.util.List<Item> items,

          @Schema(description = "페이지당 출력 수")
          int numOfRows,

          @Schema(description = "현재 페이지 번호")
          int pageNo,

          @Schema(description = "전체 건수")
          int totalCount
  ) {}

  public record Item(

          @Schema(description = "시행년도")
          String implYy,

          @Schema(description = "시행회차")
          int implSeq,

          @Schema(description = "자격구분코드")
          String qualgbCd,

          @Schema(description = "자격구분명")
          String qualgbNm,

          @Schema(description = "시행계획에 대한 설명")
          String description,

          @Schema(description = "필기 접수 시작일")
          String docRegStartDt,

          @Schema(description = "필기 접수 종료일")
          String docRegEndDt,

          @Schema(description = "필기 시험 시작일")
          String docExamStartDt,

          @Schema(description = "필기 시험 종료일")
          String docExamEndDt,

          @Schema(description = "필기 합격 발표일")
          String docPassDt,

          @Schema(description = "실기 접수 시작일")
          String pracRegStartDt,

          @Schema(description = "실기 접수 종료일")
          String pracRegEndDt,

          @Schema(description = "실기 시험 시작일")
          String pracExamStartDt,

          @Schema(description = "실기 시험 종료일")
          String pracExamEndDt,

          @Schema(description = "실기 합격 발표일")
          String pracPassDt
  ) {}
}