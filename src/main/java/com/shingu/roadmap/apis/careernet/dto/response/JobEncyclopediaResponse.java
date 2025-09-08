package com.shingu.roadmap.apis.careernet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "직업백과 API 응답 DTO")
@Getter
@Setter
@ToString
public class JobEncyclopediaResponse {

  @Schema(description = "전체 검색 결과 수", example = "128")
  private Integer count;

  @Schema(description = "현재 페이지 번호", example = "1")
  private Integer pageIndex;

  @Schema(description = "페이지당 출력 건수", example = "10")
  private Integer pageSize;

  @Schema(description = "직업 목록")
  private List<JobEncyclopediaSummary> jobs;
}