package com.shingu.roadmap.auth.service;

import com.shingu.roadmap.auth.domain.RefreshToken;
import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.auth.exception.*;
import com.shingu.roadmap.auth.repository.RefreshTokenRepository;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.security.jwt.JwtUtil;
import com.shingu.roadmap.security.jwt.TokenPayload;
import com.shingu.roadmap.security.model.CustomUserDetails;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final int REFRESH_LIFETIME_DAYS = 14;
  private static final long REFRESH_ROTATE_THRESHOLD_MINUTES = 60L; // 남은 유효기간이 60분 미만이면 회전

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final MemberRepository memberRepository;
  private final UserDetailsService userDetailsService;
  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 로그인 처리
   * - Spring Security 인증
   * - JWT 발급
   * - Account.lastLogin 도메인 메서드로 갱신
   * - RefreshToken은 Member 연관 없이 별도 테이블에 신규 저장
   */
  @Transactional
  public LoginResponse login(LoginRequest request) {
    try {
      // 1) 인증
      Authentication authentication = authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.email(), request.password())
      );
    } catch (AuthenticationException e) {
      throw new InvalidCredentialsException();
    }

    // 2) 회원 로드
    Member member = memberRepository.findByAccountEmail(request.email())
            .orElseThrow(() -> new UserNotFoundException(request.email()));

    // 3) 마지막 로그인 도메인 갱신
    member.getAccount().markLoggedIn(LocalDateTime.now());

    // 4) JWT 페이로드 구성
    TokenPayload payload = new TokenPayload(
            member.getId(),
            member.getAccount().getEmail(),
            member.getName(),
            member.getRole()
    );

    // 5) 토큰 발급
    String accessToken = jwtUtil.generateAccessToken(payload);
    String refreshToken = jwtUtil.generateRefreshToken(payload);

    // 6) RefreshToken 저장 (Member와 연관 설정)
    RefreshToken tokenEntity = RefreshToken.builder()
            .token(refreshToken)
            .expiresAt(Instant.now().plus(REFRESH_LIFETIME_DAYS, ChronoUnit.DAYS))
            .build();
    member.updateRefreshToken(tokenEntity);

    return new LoginResponse(accessToken, refreshToken);
  }

  /**
   * Refresh 토큰으로 액세스 토큰 재발급
   * - 동시 접근 제어를 위한 비관적 잠금 사용
   * - One-Time-Use 패턴 적용 (Race Condition 방지)
   * - DB에 저장된 토큰인지 확인 후 즉시 삭제
   * - 만료/서명/유형 검증
   * - Access Token 및 새로운 Refresh Token 재발급
   */
  @Transactional
  public LoginResponse refreshToken(String refreshToken) {
    // 1) 토큰으로 멤버를 찾아 잠급니다. (동시성 제어)
    Member member = memberRepository.findAndLockByRefreshToken_Token(refreshToken)
            .orElseThrow(() -> new InvalidRefreshTokenException("존재하지 않는 또는 이미 사용된 Refresh Token입니다."));

    RefreshToken tokenEntity = member.getRefreshToken();

    // 2) 토큰 유효성 검증 (null 체크 후 만료 체크)
    if (tokenEntity == null) {
      member.updateRefreshToken(null);
      throw new ExpiredRefreshTokenException();
    }
    if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
      member.updateRefreshToken(null);
      throw new ExpiredRefreshTokenException();
    }

    // 3) JWT 무결성 및 유형(refresh) 검증
    if (!jwtUtil.isValidRefreshToken(refreshToken)) {
      throw new TokenIntegrityException("Refresh Token 무결성 검증 실패");
    }

    // 4) 기존 토큰과의 연관관계를 끊습니다. (orphanRemoval=true에 의해 삭제 처리됨)
    member.updateRefreshToken(null);

    // 5) 새로운 토큰 발급
    TokenPayload payload = new TokenPayload(
            member.getId(),
            member.getEmail(),
            member.getName(),
            member.getRole()
    );
    String newAccessToken = jwtUtil.generateAccessToken(payload);
    String newRefreshToken = jwtUtil.generateRefreshToken(payload);

    // 6) 새로운 RefreshToken 엔티티 생성 및 저장
    RefreshToken newTokenEntity = RefreshToken.builder()
            .token(newRefreshToken)
    // 7) Member와 새로운 RefreshToken 연결
    member.updateRefreshToken(newTokenEntity);
    // Member의 refreshToken 참조가 저장소에 반영됩니다.

    return new LoginResponse(newAccessToken, newRefreshToken);
    // 트랜잭션 종료 시 변경 감지에 의해 Member의 refreshToken 참조가 업데이트됨

    return new LoginResponse(newAccessToken, newRefreshToken);
  }

  /**
   * 로그아웃
   * - 도메인 모델이 Member 연관을 가지지 않으므로, 클라이언트가 보유한 refreshToken 문자열로 삭제
   * - 추가로 서버 측에서 블랙리스트(선택) 등을 운영하려면 별도 테이블/캐시 도입
   */
  @Transactional
  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) return;

    // 1) 토큰을 들고 있는 멤버를 찾는다 (단방향 1:1 연관 경로 사용)
    Member member = memberRepository.findByRefreshToken_Token(refreshToken)
            .orElse(null);

    if (member != null && member.getRefreshToken() != null) {
      // 2) 연관을 끊는다 → orphanRemoval=true 덕분에 토큰 행이 자동 삭제됨
      member.updateRefreshToken(null);
      // dirty checking으로 커밋 시점에 FK null + orphan delete가 처리됨
    } else {
      // 혹시 연관을 못 찾았을 때만 직접 삭제(일반적으로는 필요 없음)
      refreshTokenRepository.findByToken(refreshToken)
              .ifPresent(refreshTokenRepository::delete);
    }
  }
}