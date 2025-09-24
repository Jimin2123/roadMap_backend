package com.shingu.roadmap.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shingu.roadmap.security.event.SecurityEventPublisher;
import com.shingu.roadmap.security.jwt.JwtUtil;
import com.shingu.roadmap.security.jwt.TokenType;
import com.shingu.roadmap.security.jwt.TokenValidationResult;
import com.shingu.roadmap.security.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
public class EnhancedJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityEventPublisher securityEventPublisher;
    private final ObjectMapper objectMapper;

    public EnhancedJwtAuthenticationFilter(JwtUtil jwtUtil,
                                           CustomUserDetailsService userDetailsService,
                                           SecurityEventPublisher securityEventPublisher,
                                           ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.securityEventPublisher = securityEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 이미 인증된 요청이면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientIp  = resolveClientIp(request);
        final String userAgent = resolveUserAgent(request);

        try {
            final String token = extractBearerToken(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            final TokenValidationResult validation = jwtUtil.validateToken(token, TokenType.ACCESS);

            if (validation.isValid()) {
                // 인증 세팅
                authenticateUser(validation.getClaims(), request);

                // 퍼블리셔 예외 무시(필터 체인 보호)
                try {
                    final String email = validation.getClaims().get("email", String.class);
                    securityEventPublisher.publishAuthenticationSuccess(email, clientIp, userAgent);
                } catch (Exception pubEx) {
                    log.warn("auth success publish failed: {}", pubEx.toString());
                }

                filterChain.doFilter(request, response);
                return;
            }

            // 토큰 만료는 재발급을 위한 정상 시나리오일 수 있으므로, 요청을 계속 진행시켜 permitAll() 경로에 대한 접근을 허용한다.
            if (validation.getErrorType() == TokenValidationResult.ValidationError.EXPIRED) {
                SecurityContextHolder.clearContext(); // 컨텍스트는 비워줌
                filterChain.doFilter(request, response);
                return;
            }

            // 만료 외 다른 모든 토큰 오류(형식, 서명 등)는 비정상으로 간주하고 즉시 실패 처리
            handleAuthenticationFailure(response, validation, clientIp, userAgent);

        } catch (Exception e) {
            log.error("Unexpected error in JWT filter", e);

            try {
                securityEventPublisher.publishAuthenticationError(clientIp, userAgent, e.getMessage());
            } catch (Exception pubEx) {
                log.warn("auth error publish failed: {}", pubEx.toString());
            }

            handleUnexpectedError(response);
            return;
        }
    }

    /** Authorization: Bearer <token> 안전 추출 (대소문자/공백 허용) */
    @Nullable
    private String extractBearerToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) return null;
        auth = auth.trim();
        if (auth.length() < 8) return null; // "Bearer X" 최소 길이
        // case-insensitive prefix
        if (auth.regionMatches(true, 0, "Bearer", 0, 6)) {
            String value = auth.substring(6).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private void authenticateUser(Claims claims, HttpServletRequest request) {
        try {
            final String email = claims.get("email", String.class);
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("JWT claim 'email' is missing");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Failed to authenticate user: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            throw e;
        }
    }

    private void handleAuthenticationFailure(HttpServletResponse response,
                                             TokenValidationResult result,
                                             String clientIp,
                                             String userAgent) throws IOException {

        SecurityContextHolder.clearContext();

        // 퍼블리셔 예외가 체인을 깨지 않도록 가드
        try {
            String reason = result.getErrorType() != null
                    ? result.getErrorType().name()
                    : "UNKNOWN";
            securityEventPublisher.publishAuthenticationFailure(reason, clientIp, userAgent);
        } catch (Exception pubEx) {
            log.warn("auth failure publish failed: {}", pubEx.toString());
        }

        ErrorResponse body = createErrorResponse(result.getErrorType());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void handleUnexpectedError(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();

        ErrorResponse body = new ErrorResponse("AUTHENTICATION_ERROR", "Authentication failed", LocalDateTime.now());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 정책상 401 유지
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private ErrorResponse createErrorResponse(@Nullable TokenValidationResult.ValidationError errorType) {
        String code;
        String msg;

        if (errorType == null) {
            code = "TOKEN_ERROR";
            msg  = "Authentication failed";
        } else {
            switch (errorType) {
                case EXPIRED -> { code = "TOKEN_EXPIRED";      msg = "Token has expired"; }
                case MALFORMED -> { code = "TOKEN_MALFORMED";  msg = "Token format is invalid"; }
                case SIGNATURE_INVALID -> { code = "TOKEN_INVALID"; msg = "Token is invalid"; }
                case UNSUPPORTED -> { code = "TOKEN_UNSUPPORTED";   msg = "Unsupported token type"; }
                default -> { code = "TOKEN_ERROR"; msg = "Authentication failed"; }
            }
        }
        return new ErrorResponse(code, msg, LocalDateTime.now());
    }

    /** XFF 등 신뢰 가능한 헤더 우선 → 없으면 RemoteAddr */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP",
                "True-Client-IP", "Forwarded"
        };
        for (String h : headerNames) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                // XFF: "client, proxy1, proxy2" → 첫 값
                String first = v.split(",")[0].trim();
                if (!first.isBlank()) return first;
            }
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return (ua == null || ua.isBlank()) ? "unknown" : ua;
    }

    // ===== 응답 DTO =====
    @Getter
    @ToString
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String code;
        private final String message;
        private final LocalDateTime timestamp;
    }
}
