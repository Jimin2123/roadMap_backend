package com.shingu.roadmap.auth.exception;

public class TokenIntegrityException extends AuthException {

    private static final String ERROR_CODE = "AUTH_005";

    public TokenIntegrityException() {
        super("토큰 무결성 검증에 실패했습니다.");
    }

    public TokenIntegrityException(String message) {
        super(message);
    }

    public TokenIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}