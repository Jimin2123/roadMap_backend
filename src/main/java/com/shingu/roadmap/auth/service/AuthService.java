package com.shingu.roadmap.auth.service;

import com.shingu.roadmap.auth.dto.request.LoginRequest;
import com.shingu.roadmap.auth.dto.response.LoginResponse;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.security.jwt.JwtUtil;
import com.shingu.roadmap.security.jwt.TokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final MemberRepository memberRepository;

  public LoginResponse login(LoginRequest loginRequest) {

    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
    );

    // 2. 인증에 성공한 사용자 정보 가져오기
    Member member = memberRepository.findByAccountEmail(loginRequest.email())
            .orElseThrow(() -> new IllegalArgumentException("이메일에 해당하는 사용자를 찾을 수 없습니다."));

    TokenPayload payload = new TokenPayload(
            member.getId(),
            member.getAccount().getEmail(),
            member.getName(),
            member.getRole()
    );

    String accessToken = jwtUtil.generateAccessToken(payload);
    String refreshToken = jwtUtil.generateRefreshToken(payload);

    return new LoginResponse(accessToken, refreshToken);
  }
}
