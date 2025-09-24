package com.shingu.roadmap.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 보안 관련 에러 응답 표준 포맷
 *
 * 인증/인가 실패 시 일관된 응답 형식을 제공하여 클라이언트에서
 * 통일된 방식으로 에러를 처리할 수 있도록 합니다.
 */
@Getter
@ToString
@AllArgsConstructor
public class SecurityErrorResponse {
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    /**
     * 인증 실패 응답 생성
     */
    public static SecurityErrorResponse authenticationFailed(String message) {
        return new SecurityErrorResponse("AUTHENTICATION_FAILED", message, LocalDateTime.now());
    }

    /**
     * 인가 실패 응답 생성
     */
    public static SecurityErrorResponse accessDenied(String message) {
        return new SecurityErrorResponse("ACCESS_DENIED", message, LocalDateTime.now());
    }

    /**
     * 토큰 관련 에러 응답 생성
     */
    public static SecurityErrorResponse tokenError(String code, String message) {
        return new SecurityErrorResponse(code, message, LocalDateTime.now());
    }

    /**
     * 일반 보안 에러 응답 생성
     */
    public static SecurityErrorResponse securityError(String code, String message) {
        return new SecurityErrorResponse(code, message, LocalDateTime.now());
    }
}