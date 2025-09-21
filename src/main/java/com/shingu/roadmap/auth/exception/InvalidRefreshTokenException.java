package com.shingu.roadmap.auth.exception;

public class InvalidRefreshTokenException extends AuthException {

    private static final String ERROR_CODE = "AUTH_003";

    public InvalidRefreshTokenException() {
        super("유효하지 않은 Refresh Token입니다.");
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}