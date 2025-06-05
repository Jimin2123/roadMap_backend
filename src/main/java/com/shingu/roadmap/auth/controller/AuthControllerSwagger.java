package com.shingu.roadmap.auth.controller;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}
