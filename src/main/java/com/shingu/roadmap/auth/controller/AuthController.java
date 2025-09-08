package com.shingu.roadmap.auth.controller;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.auth.service.AuthService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerSwagger {

  private final AuthService authService;

  private static final String REFRESH_COOKIE_NAME = "refreshToken";
  private static final int REFRESH_COOKIE_MAX_AGE_SEC = 14 * 24 * 60 * 60;

  @Override
  @PostMapping("/api/v1/auth/login")
  public ResponseEntity<LoginResponse> login(
          @RequestBody LoginRequest loginRequest,
          HttpServletResponse response,
          HttpServletRequest request
  ) {
    LoginResponse tokens = authService.login(loginRequest);

    // refresh 토큰을 HttpOnly 쿠키로 저장
    Cookie refreshTokenCookie = buildRefreshTokenCookie(
            tokens.refreshToken(),
            /*delete*/ false,
            /*secure*/ request.isSecure()
    );
    response.addCookie(refreshTokenCookie);

    // 액세스 토큰만 바디로 반환
    return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), null));
  }

  @Override
  @PostMapping("/api/v1/auth/refreshToken")
  public ResponseEntity<LoginResponse> refreshToken(
          HttpServletRequest request,
          HttpServletResponse response
  ) {
    String refreshToken = extractRefreshTokenFromCookie(request);
    if (refreshToken == null || refreshToken.isBlank()) {
      return ResponseEntity.status(401).build();
    }

    LoginResponse tokens = authService.refreshToken(refreshToken);

    // 회전이 발생하면 쿠키도 교체
    if (tokens.refreshToken() != null) {
      Cookie refreshTokenCookie = buildRefreshTokenCookie(
              tokens.refreshToken(),
              /*delete*/ false,
              /*secure*/ request.isSecure()
      );
      response.addCookie(refreshTokenCookie);
      return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), null));
    }

    // 회전이 없으면 액세스 토큰만 반환
    return ResponseEntity.ok(new LoginResponse(tokens.accessToken(), null));
  }

  @PostMapping("/api/v1/auth/logout")
  public ResponseEntity<Void> logout(
          @AuthenticationPrincipal CustomUserDetails userDetails, // 사용은 선택적(감사 로그용)
          HttpServletRequest request,
          HttpServletResponse response
  ) {
    String refreshToken = extractRefreshTokenFromCookie(request);
    if (refreshToken != null && !refreshToken.isBlank()) {
      // 도메인/서비스 구조상 memberId가 아닌 refreshToken 문자열로 삭제
      authService.logout(refreshToken);
    }

    // 쿠키 제거
    Cookie deleteCookie = buildRefreshTokenCookie(
            null,
            /*delete*/ true,
            /*secure*/ request.isSecure()
    );
    response.addCookie(deleteCookie);

    return ResponseEntity.ok().build();
  }

  /* ===================== 헬퍼 ===================== */

  private Cookie buildRefreshTokenCookie(String value, boolean delete, boolean secure) {
    Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, delete ? null : value);
    cookie.setHttpOnly(true);
    cookie.setSecure(secure); // HTTPS 환경에서 true
    cookie.setPath("/");

    if (delete) {
      cookie.setMaxAge(0);
    } else {
      cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE_SEC);
    }

    // SameSite 설정이 필요하면 ResponseCookie 사용으로 전환 권장
    // (기본 Cookie API에는 SameSite 속성이 없음)
    return cookie;
  }

  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) return null;
    for (Cookie cookie : request.getCookies()) {
      if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}