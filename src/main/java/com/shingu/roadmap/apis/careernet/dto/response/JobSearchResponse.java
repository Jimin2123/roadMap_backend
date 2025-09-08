package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "직업 정보 검색 결과 응답 DTO")
@Getter
@Setter
public class JobSearchResponse {

  @Schema(description = "전체 검색 결과 수", example = "45")
  private String totalCount;

  @Schema(description = "직업 검색 결과 목록")
  private List<JobSearchSummary> content;
}