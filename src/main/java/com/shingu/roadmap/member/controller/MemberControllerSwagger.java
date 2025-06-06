package com.shingu.roadmap.member.controller;

import com.shingu.roadmap.member.dto.request.MemberRequest;
import com.shingu.roadmap.member.dto.request.ProfileRequest;
import com.shingu.roadmap.member.dto.response.MemberResponse;
import com.shingu.roadmap.member.dto.response.ProfileResponse;
import com.shingu.roadmap.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member API", description = "회원 관련 API")
public interface MemberControllerSwagger {

  @Operation(
          summary = "회원 가입",
          description = "회원 정보를 입력하여 회원 가입을 진행합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "회원 가입 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<MemberResponse> signUp(MemberRequest memberRequest);

  @SecurityRequirement(name = "bearerAuth")
  @Operation(
          summary = "사용자 프로필 정보 추가",
          description = "사용자 프로필 정보를 추가 합니다. 그리고 openAI를 통해 사용자 정보 기반 NCS 코드를 발급해 줍니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "사용자 프로필 추가 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<MemberResponse> updateProfile(CustomUserDetails userDetails, ProfileRequest profileRequest);

  @SecurityRequirement(name = "bearerAuth")
  @Operation(
          summary = "사용자 정보 조회",
          description = "사용자 정보를 조회합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "사용자 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<MemberResponse> getMember(CustomUserDetails userDetails);

  @SecurityRequirement(name = "bearerAuth")
  @Operation(
          summary = "사용자 프로필 정보 조회",
          description = "사용자 프로필 정보를 조회합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "사용자 프로필 정보 조회 성공",
                          content = @Content(schema = @Schema(implementation = MemberResponse.class))
                  ),
          }
  )
  ResponseEntity<ProfileResponse> getProfile(CustomUserDetails userDetails);
}
