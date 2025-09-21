package com.shingu.roadmap.auth.exception;

public enum ErrorCode {
    // 인증 관련 에러 코드
    AUTH_001("AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_002("AUTH_002", "존재하지 않는 사용자입니다."),
    AUTH_003("AUTH_003", "유효하지 않은 Refresh Token입니다."),
    AUTH_004("AUTH_004", "Refresh Token이 만료되었습니다."),
    AUTH_005("AUTH_005", "토큰 무결성 검증에 실패했습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}