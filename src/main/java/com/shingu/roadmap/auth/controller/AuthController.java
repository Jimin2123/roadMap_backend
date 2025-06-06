package com.shingu.roadmap.auth.controller;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerSwagger {

  private final AuthService authService;

  @Override
  @PostMapping("/api/v1/auth/login")
  public ResponseEntity<LoginResponse> login(
          @RequestBody LoginRequest loginRequest,
          HttpServletResponse response
  ) {
    LoginResponse tokens = authService.login(loginRequest);

    Cookie refreshTokenCookie = setRefreshTokenCookie(tokens.refreshToken(), response);
    response.addCookie(refreshTokenCookie);

    return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), null));
  }

  @Override
  @PostMapping("/api/v1/auth/refreshToken")
  public ResponseEntity<LoginResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
    String refreshToken = extractRefreshTokenFromCookie(request);
    LoginResponse tokens = authService.refreshToken(refreshToken);
    if(tokens.refreshToken() != null) {
      Cookie refreshTokenCookie = setRefreshTokenCookie(tokens.refreshToken(), response);
      response.addCookie(refreshTokenCookie);

      return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), null));
    }
    return ResponseEntity.ok(tokens);
  }

  private Cookie setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
    Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setSecure(false); // HTTPS 환경이면 true
    refreshTokenCookie.setPath("/");
    refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일

    return refreshTokenCookie;
  }

  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if ("refreshToken".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
