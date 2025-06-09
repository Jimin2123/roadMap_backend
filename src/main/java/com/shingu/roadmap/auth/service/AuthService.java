package com.shingu.roadmap.auth.service;

import com.shingu.roadmap.auth.domain.RefreshToken;
import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.auth.repository.RefreshTokenRepository;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.security.jwt.JwtUtil;
import com.shingu.roadmap.security.jwt.TokenPayload;
import com.shingu.roadmap.security.model.CustomUserDetails;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final MemberRepository memberRepository;
  private final UserDetailsService userDetailsService;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public LoginResponse login(LoginRequest loginRequest) {

    // Spring Security 인증 처리
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                    loginRequest.email(), loginRequest.password()
            )
    );

    // 인증된 사용자 정보 조회
    Member member = memberRepository.findByAccountEmail(loginRequest.email())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    // JWT 페이로드 구성
    TokenPayload payload = new TokenPayload(
            member.getId(),
            member.getAccount().getEmail(),
            member.getName(),
            member.getRole()
    );

    // Access & Refresh Token 생성
    String accessToken = jwtUtil.generateAccessToken(payload);
    String refreshToken = jwtUtil.generateRefreshToken(payload);

    // 기존 RefreshToken이 있다면 업데이트 / 없으면 새로 생성
    RefreshToken existingToken = member.getRefreshToken();

    if (existingToken != null) {
      existingToken.setToken(refreshToken);
      existingToken.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
    } else {
      RefreshToken newToken = new RefreshToken(
              null,
              refreshToken,
              Instant.now().plus(14, ChronoUnit.DAYS)
      );
      member.setRefreshToken(newToken);
    }

    // Member 저장 (Cascade로 RefreshToken 자동 저장)
    memberRepository.save(member);

    return new LoginResponse(accessToken, refreshToken);
  }

  public LoginResponse refreshToken(String refreshToken) {

    RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("유효하지 않은 Refresh Token"));

    if (refreshTokenEntity.getExpiresAt().isBefore(Instant.now())) {
      refreshTokenRepository.delete(refreshTokenEntity);
      throw new RuntimeException("Refresh Token 만료됨");
    }

    if (!jwtUtil.isValidRefreshToken(refreshToken)) {
      throw new RuntimeException("Refresh Token 무결성 검증 실패");
    }

    Claims claims = jwtUtil.parseClaims("refresh", refreshToken);
    String email = claims.get("email", String.class);

    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
    Member member = ((CustomUserDetails) userDetails).getMember();

    TokenPayload payload = new TokenPayload(
            member.getId(), email, member.getName(), member.getRole()
    );

    String newAccessToken = jwtUtil.generateAccessToken(payload);

    long remaining = Duration.between(Instant.now(), refreshTokenEntity.getExpiresAt()).toMinutes();
    if (remaining < 60) { // 예: 1시간 미만이면 새로 발급
      String newRefreshToken = jwtUtil.generateRefreshToken(payload);
      refreshTokenEntity.setToken(newRefreshToken);
      refreshTokenEntity.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
      refreshTokenRepository.save(refreshTokenEntity);
      return new LoginResponse(newAccessToken, newRefreshToken);
    }

    return new LoginResponse(newAccessToken, null);
  }

  public void logout(Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

    RefreshToken refreshToken = member.getRefreshToken();
    if (refreshToken != null) {
      refreshTokenRepository.delete(refreshToken);
      member.setRefreshToken(null);
      memberRepository.save(member);
    }
  }
}
