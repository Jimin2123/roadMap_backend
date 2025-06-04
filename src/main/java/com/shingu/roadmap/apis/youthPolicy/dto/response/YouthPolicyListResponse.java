package com.shingu.roadmap.apis.youthPolicy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "청년 정책 리스트 응답 DTO")
public record YouthPolicyListResponse(
        @Schema(description = "결과 코드")
        int resultCode,

        @Schema(description = "결과 메시지")
        String resultMessage,

        @Schema(description = "결과 DTO")
        ResultResponse result

) {
  @Schema(description = "결과 DTO")
  public record ResultResponse(
    @Schema(description = "페이징 정보")
    PaggingResponse pagging,

    @Schema(description = "청년 정책 리스트")
    List<YouthPolicyItemResponse> youthPolicyList
  ) {}

  @Schema(description = "페이징 응답 DTO")
  public record PaggingResponse(
          @Schema(description = "현재 페이지")
          int pageNum,

          @Schema(description = "페이지당 출력 개수")
          int pageSize,

          @Schema(description = "전체 건수")
          int totCount
  ) { }
}
