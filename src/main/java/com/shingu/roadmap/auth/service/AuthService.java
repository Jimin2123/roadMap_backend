package com.shingu.roadmap.auth.service;

import com.shingu.roadmap.auth.domain.RefreshToken;
import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.auth.exception.*;
import com.shingu.roadmap.auth.repository.RefreshTokenRepository;
import com.shingu.roadmap.common.exception.CustomException;
import com.shingu.roadmap.common.exception.ErrorCode;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    // 1) 인증
    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(request.email(), request.password())
      );
    } catch (BadCredentialsException e) {
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    } catch (UsernameNotFoundException e) {
      throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
    } catch (AuthenticationException e) {
      // 기타 인증 예외 처리
      throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "인증 중 알 수 없는 오류가 발생했습니다.");
//     try {
//       // 1) 인증
//       Authentication authentication = authenticationManager.authenticate(
//               new UsernamePasswordAuthenticationToken(request.email(), request.password())
//       );
//     } catch (AuthenticationException e) {
//       throw new InvalidCredentialsException();
    }

    // 2) 회원 로드 (인증 성공 후에도 혹시 모를 경우를 대비)
    Member member = memberRepository.findByAccountEmail(request.email())
            .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
//             .orElseThrow(() -> new UserNotFoundException(request.email()));

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

    // 6) RefreshToken 저장 (연관 없음 → 단독 저장)
    RefreshToken tokenEntity = RefreshToken.builder()
            .token(refreshToken)
            .expiresAt(Instant.now().plus(REFRESH_LIFETIME_DAYS, ChronoUnit.DAYS))
            .build();
    refreshTokenRepository.save(tokenEntity);

    return new LoginResponse(accessToken, refreshToken);
  }

  /**
   * Refresh 토큰으로 액세스 토큰 재발급
   * - DB에 저장된 토큰인지 확인
   * - 만료/서명/유형 검증
   * - Access Token 재발급
   * - 남은 만료시간이 임계 미만이면 Refresh 토큰 회전(rotate + renewTo)
   */
  @Transactional
  public LoginResponse refreshToken(String refreshToken) {
    // 1) 존재/만료 확인
    RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다."));

    if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
      refreshTokenRepository.delete(tokenEntity);
      throw new CustomException(ErrorCode.INVALID_TOKEN, "Refresh Token이 만료되었습니다.");
//             .orElseThrow(() -> new InvalidRefreshTokenException());

//     if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
//       refreshTokenRepository.delete(tokenEntity);
//       throw new ExpiredRefreshTokenException();
    }

    // 2) JWT 무결성 및 유형(refresh) 검증
    if (!jwtUtil.isValidRefreshToken(refreshToken)) {
      throw new CustomException(ErrorCode.INVALID_TOKEN, "Refresh Token 무결성 검증에 실패했습니다.");

// //       throw new TokenIntegrityException("Refresh Token 무결성 검증 실패");
//     }

//     Claims claims;
//     try {
//       claims = jwtUtil.parseClaims("refresh", refreshToken);
//     } catch (Exception e) {
//       throw new TokenIntegrityException("토큰 파싱 실패", e);
    }

    String email = claims.get("email", String.class);

    // 3) 사용자 로드
    UserDetails userDetails;
    try {
      userDetails = userDetailsService.loadUserByUsername(email);
    } catch (Exception e) {
      throw new UserNotFoundException(email);
    }

    Member member = ((CustomUserDetails) userDetails).getMember();

    TokenPayload payload = new TokenPayload(
            member.getId(),
            email,
            member.getName(),
            member.getRole()
    );

    // 4) Access 토큰 재발급
    String newAccessToken = jwtUtil.generateAccessToken(payload);

    // 5) Refresh 회전 조건 검사 및 처리
    long remainingMinutes = Duration.between(Instant.now(), tokenEntity.getExpiresAt()).toMinutes();
    if (remainingMinutes < REFRESH_ROTATE_THRESHOLD_MINUTES) {
      String rotated = jwtUtil.generateRefreshToken(payload);
      tokenEntity.rotate(rotated);
      tokenEntity.renewTo(Instant.now().plus(REFRESH_LIFETIME_DAYS, ChronoUnit.DAYS));
      refreshTokenRepository.save(tokenEntity);
      return new LoginResponse(newAccessToken, rotated);
    }

    return new LoginResponse(newAccessToken, null);
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