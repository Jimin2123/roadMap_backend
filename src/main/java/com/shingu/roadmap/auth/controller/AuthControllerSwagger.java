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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth API", description = "사용자 인증 관련 API")
public interface AuthControllerSwagger {

  @Operation(
          summary = "로그인",
          description = "사용자 로그인을 위한 API입니다. 성공 시 AccessToken을 본문으로, RefreshToken을 HttpOnly 쿠키로 반환합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "로그인 성공",
                          content = @Content(schema = @Schema(implementation = LoginResponse.class))
                  )
          }
  )
  ResponseEntity<LoginResponse> login(
          @RequestBody LoginRequest loginRequest,
          HttpServletResponse response,
          HttpServletRequest request
  );

  @Operation(
          summary = "리프레시 토큰 갱신",
          description = "쿠키에 저장된 리프레시 토큰을 사용하여 새로운 액세스 토큰(필요 시 리프레시 토큰 회전)을 발급합니다.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "토큰 갱신 성공",
                          content = @Content(schema = @Schema(implementation = LoginResponse.class))
                  ),
                  @ApiResponse(
                          responseCode = "401",
                          description = "리프레시 토큰 누락/무효/만료"
                  )
          }
  )
  ResponseEntity<LoginResponse> refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  );

  @Operation(
          summary = "로그아웃",
          description = "사용자를 로그아웃시키고 리프레시 토큰 쿠키 및 서버 저장 토큰을 삭제합니다.",
          responses = {
                  @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                  @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
          }
  )
  ResponseEntity<Void> logout(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          HttpServletRequest request,
          HttpServletResponse response
  );
}