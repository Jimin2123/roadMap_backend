package com.shingu.roadmap.apis.careernet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "직업 정보 API 요청 DTO")
@Getter
@Setter
@ToString
public class JobInformationRequest {


  @NotEmpty
  @Schema(description = "서비스 타입", requiredMode = Schema.RequiredMode.REQUIRED, example = "api", defaultValue = "api")
  private String svcType = "api";

  @NotEmpty
  @Schema(description = "서비스 코드 (JOB: 리스트, JOB_VIEW: 상세)", requiredMode = Schema.RequiredMode.REQUIRED, example = "JOB")
  private String svcCode;

  @Schema(description = "응답 형식 (xml, json)", example = "json", defaultValue = "json")
  private String contentType = "json";

  @NotEmpty
  @Schema(description = "직업사전 분류 형태 코드 (job_dic_list: 직업사전직업분류, job_apti_list: 적성유형별)", requiredMode = Schema.RequiredMode.REQUIRED, example = "job_dic_list")
  private String gubun;

  @Schema(description = "능력", example = "전체")
  private String pgubn;

  @Schema(description = "직업 분류 또는 직군", example = "전체")
  private String category;

  @Schema(description = "현재 페이지", example = "1")
  private String thisPage;

  @Schema(description = "한 페이지당 건수", example = "10")
  private String perPage;

  @Schema(description = "검색어", example = "개발자")
  private String searchJobNm;

  @Schema(description = "직업 코드 ID (상세 조회 시 사용)", example = "10038")
  private String jobdicSeq;
}