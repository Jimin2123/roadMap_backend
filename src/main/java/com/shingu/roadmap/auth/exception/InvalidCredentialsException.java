package com.shingu.roadmap.auth.exception;

public class InvalidCredentialsException extends AuthException {

    private static final String ERROR_CODE = "AUTH_001";

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}