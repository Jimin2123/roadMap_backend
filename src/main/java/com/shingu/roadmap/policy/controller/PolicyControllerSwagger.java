package com.shingu.roadmap.policy.controller;

import com.shingu.roadmap.apis.youthPolicy.dto.response.YouthPolicyListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Policy API", description = "정책 정보 관련 API")
public interface PolicyControllerSwagger {
  @Operation(
          summary = "정책 조회",
          description = "온통청년 API를 통해 정책 정보를 조회합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "정책 조회 성공",
                          content = @Content(schema = @Schema(implementation = YouthPolicyListResponse.class))
                  ),
          }
  )
  YouthPolicyListResponse getYouthPolicyList();
}
