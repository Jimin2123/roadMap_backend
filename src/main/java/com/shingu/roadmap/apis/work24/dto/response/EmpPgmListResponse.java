package com.shingu.roadmap.apis.work24.dto.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "고용센터 프로그램 일정 응답 DTO")
public record EmpPgmListResponse(

        @Schema(description = "총 개수")
        @JacksonXmlProperty(localName = "total")
        int total,

        @Schema(description = "시작 페이지")
        @JacksonXmlProperty(localName = "startPage")
        int startPage,

        @Schema(description = "페이지당 표시 수")
        @JacksonXmlProperty(localName = "display")
        int display,

        @Schema(description = "프로그램 목록")
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "empPgmSchdInvite")
        List<EmpPgmSchdInvite> empPgmSchdInvite

) {
  @Schema(description = "고용센터 프로그램 개별 항목")
  public record EmpPgmSchdInvite(

          @JacksonXmlProperty(localName = "orgNm")
          String orgNm,

          @JacksonXmlProperty(localName = "pgmNm")
          String pgmNm,

          @JacksonXmlProperty(localName = "pgmSubNm")
          String pgmSubNm,

          @JacksonXmlProperty(localName = "pgmTarget")
          String pgmTarget,

          @JacksonXmlProperty(localName = "pgmStdt")
          String pgmStdt, // YYYYMMDD 형식

          @JacksonXmlProperty(localName = "pgmEndt")
          String pgmEndt,

          @JacksonXmlProperty(localName = "openTimeClcd")
          String openTimeClcd,

          @JacksonXmlProperty(localName = "openTime")
          String openTime,

          @JacksonXmlProperty(localName = "operationTime")
          String operationTime,

          @JacksonXmlProperty(localName = "openPlcCont")
          String openPlcCont

  ) {}
}