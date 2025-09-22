package com.shingu.roadmap.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException e) {
        log.warn("로그인 실패: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e) {
        log.warn("사용자 조회 실패: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        log.warn("유효하지 않은 Refresh Token: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredRefreshToken(ExpiredRefreshTokenException e) {
        log.warn("만료된 Refresh Token: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenIntegrityException.class)
    public ResponseEntity<Map<String, Object>> handleTokenIntegrity(TokenIntegrityException e) {
        log.error("토큰 무결성 검증 실패: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException e) {
        log.error("인증 오류: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(AuthException e, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("errorCode", e.getErrorCode());

        return ResponseEntity.status(status).body(errorResponse);
    }
}