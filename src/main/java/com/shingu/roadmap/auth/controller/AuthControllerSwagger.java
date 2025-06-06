package com.shingu.roadmap.auth.controller;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth API", description = "사용자 인증 관련 API")
public interface AuthControllerSwagger {

  @Operation(
          summary = "로그인",
          description = "사용자 로그인을 위한 API입니다. 성공 시 AccessToken과 RefreshToken을 반환합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "로그인 성공",
                          content = @Content(schema = @Schema(implementation = LoginResponse.class))
                  )
          }
  )
  ResponseEntity<LoginResponse> login(LoginRequest loginRequest, HttpServletResponse response);


  @Operation(
          summary = "리프레시 토큰 갱신",
          description = "쿠키에 저장된 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급합니다",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "토큰 갱신 성공",
                          content = @Content(schema = @Schema(implementation = LoginResponse.class))
                  ),
                  @ApiResponse(
                          responseCode = "403",
                          description = "리프레시 토큰이 유효하지 않거나 만료됨"
                  )
          }
  )
  ResponseEntity<LoginResponse> refreshToken(HttpServletRequest request, HttpServletResponse response);

  @Operation(
          summary = "로그아웃",
          description = "사용자를 로그아웃시키고 리프레시 토큰을 삭제합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "로그아웃 성공"
                  ),
                  @ApiResponse(
                          responseCode = "403",
                          description = "인증되지 않은 사용자"
                  )
          }
  )
  ResponseEntity<Void> logout(CustomUserDetails userDetails, HttpServletResponse response);
}
