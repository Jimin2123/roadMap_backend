package com.shingu.roadmap.auth.exception;

public class ExpiredRefreshTokenException extends AuthException {

    private static final String ERROR_CODE = "AUTH_004";

    public ExpiredRefreshTokenException() {
        super("Refresh Token이 만료되었습니다.");
    }

    public ExpiredRefreshTokenException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}