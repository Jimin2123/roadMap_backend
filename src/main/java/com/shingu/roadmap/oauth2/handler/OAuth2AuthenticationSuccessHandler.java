package com.shingu.roadmap.oauth2.handler;

import com.shingu.roadmap.auth.domain.RefreshToken;
import com.shingu.roadmap.member.domain.Member;
import com.shingu.roadmap.member.repository.MemberRepository;
import com.shingu.roadmap.security.jwt.JwtUtil;
import com.shingu.roadmap.security.jwt.TokenPayload;
import com.shingu.roadmap.security.model.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import jakarta.servlet.http.Cookie;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Member member = memberRepository.findByAccountEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        TokenPayload payload = new TokenPayload(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole()
        );

        String accessToken = jwtUtil.generateAccessToken(payload);
        String refreshToken = jwtUtil.generateRefreshToken(payload);

        RefreshToken tokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .expiresAt(Instant.now().plus(14, ChronoUnit.DAYS))
                .build();
        member.updateRefreshToken(tokenEntity);
        memberRepository.save(member);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // HTTPS에서만 전송
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) Duration.ofDays(14).toSeconds());
        response.addCookie(refreshTokenCookie);

        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth/redirect")
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
