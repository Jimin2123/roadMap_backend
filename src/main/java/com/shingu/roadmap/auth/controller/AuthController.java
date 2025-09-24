package com.shingu.roadmap.auth.controller;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.auth.service.AuthService;
import com.shingu.roadmap.security.model.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerSwagger {

  private final AuthService authService;
  private final Environment environment;

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

    // refresh 토큰을 보안 강화된 HttpOnly 쿠키로 저장
    ResponseCookie refreshTokenCookie = buildSecureRefreshTokenCookie(
            tokens.refreshToken(),
            /*delete*/ false,
            /*secure*/ request.isSecure()
    );
    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

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
      ResponseCookie refreshTokenCookie = buildSecureRefreshTokenCookie(
              tokens.refreshToken(),
              /*delete*/ false,
              /*secure*/ request.isSecure()
      );
      response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
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
    ResponseCookie deleteCookie = buildSecureRefreshTokenCookie(
            null,
            /*delete*/ true,
            /*secure*/ request.isSecure()
    );
    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

    return ResponseEntity.ok().build();
  }

  /* ===================== 헬퍼 ===================== */

  /**
   * CSRF 공격 방어를 위한 보안 강화된 Refresh Token 쿠키 생성
   * - SameSite 속성으로 CSRF 공격 차단
   * - 환경별 차별화된 보안 정책 적용
   */
  private ResponseCookie buildSecureRefreshTokenCookie(String value, boolean delete, boolean secure) {
    ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(REFRESH_COOKIE_NAME, delete ? "" : value)
            .httpOnly(true)
            .secure(secure)
            .path("/");

    if (delete) {
      cookieBuilder.maxAge(Duration.ZERO);
    } else {
      cookieBuilder.maxAge(Duration.ofSeconds(REFRESH_COOKIE_MAX_AGE_SEC));
    }

    // 환경별 SameSite 정책 차별화
    if (isProductionEnvironment()) {
      // 프로덕션: 엄격한 CSRF 보안 정책
      cookieBuilder.sameSite("Strict");
    } else {
      // 개발환경: 개발 편의를 위한 완화된 정책
      cookieBuilder.sameSite("Lax");
    }

    return cookieBuilder.build();
  }

  /**
   * 프로덕션 환경 여부 확인
   */
  private boolean isProductionEnvironment() {
    String[] activeProfiles = environment.getActiveProfiles();
    return Arrays.asList(activeProfiles).contains("prod") ||
           Arrays.asList(activeProfiles).contains("production");
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