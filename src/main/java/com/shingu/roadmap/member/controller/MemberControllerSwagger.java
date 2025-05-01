package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member API", description = "회원 관련 API")
public interface MemberControllerSwagger {
  @Operation(
          summary = "회원 프로필 정보 추가",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "회원 프로필 추가 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<MemberResponse> updateProfile(Long id, ProfileRequest profileRequest);
}
