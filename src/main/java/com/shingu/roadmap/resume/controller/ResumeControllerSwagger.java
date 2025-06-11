package com.shingu.roadmap.resume.controller;

import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Resume API", description = "이력서 관련 API")
public interface ResumeControllerSwagger {

  @SecurityRequirement(name = "bearerAuth")
  @Operation(
          summary = "이력서 작성",
          description = "사용자가 이력서를 작성합니다. 이력서 작성 후, 해당 정보를 기반으로 추천 직무를 제공합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "이력서 작성 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<MemberResponse> createResume(CustomUserDetails userDetails, ProfileRequest request);

  @Operation(
          summary = "이력서 조회",
          description = "사용자가 작성한 이력서를 조회합니다."
  )
  ResponseEntity<Void> getResume();

  @Operation(
          summary = "이력서 수정",
          description = "사용자가 작성한 이력서를 수정합니다."
  )
  ResponseEntity<Void> updateResume();
}
