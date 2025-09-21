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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

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

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            String token = extractTokenFromRequest(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            TokenValidationResult validationResult = jwtUtil.validateToken(token, TokenType.ACCESS);

            if (validationResult.isValid()) {
                authenticateUser(validationResult.getClaims(), request);
                securityEventPublisher.publishAuthenticationSuccess(
                    validationResult.getClaims().get("email", String.class),
                    clientIp,
                    userAgent
                );
            } else {
                handleAuthenticationFailure(response, validationResult, clientIp, userAgent);
                return;
            }

        } catch (Exception e) {
            log.error("Unexpected error in JWT filter", e);
            securityEventPublisher.publishAuthenticationError(clientIp, userAgent, e.getMessage());
            handleUnexpectedError(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void authenticateUser(Claims claims, HttpServletRequest request) {
        try {
            String email = claims.get("email", String.class);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

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

        // 보안 이벤트 발행
        securityEventPublisher.publishAuthenticationFailure(
            result.getErrorType().name(),
            clientIp,
            userAgent
        );

        // 클라이언트별 에러 응답 (내부 정보 노출 방지)
        ErrorResponse errorResponse = createErrorResponse(result.getErrorType());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private void handleUnexpectedError(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();

        ErrorResponse errorResponse = new ErrorResponse("AUTHENTICATION_ERROR", "Authentication failed");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private ErrorResponse createErrorResponse(TokenValidationResult.ValidationError errorType) {
        return switch (errorType) {
            case EXPIRED -> new ErrorResponse("TOKEN_EXPIRED", "Token has expired");
            case MALFORMED -> new ErrorResponse("TOKEN_MALFORMED", "Token format is invalid");
            case SIGNATURE_INVALID -> new ErrorResponse("TOKEN_INVALID", "Token is invalid");
            case UNSUPPORTED -> new ErrorResponse("TOKEN_UNSUPPORTED", "Unsupported token type");
            default -> new ErrorResponse("TOKEN_ERROR", "Authentication failed");
        };
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp = LocalDateTime.now();

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}