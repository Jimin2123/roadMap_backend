package com.shingu.roadmap.auth.exception;

public class UserNotFoundException extends AuthException {

    private static final String ERROR_CODE = "AUTH_002";

    public UserNotFoundException() {
        super("존재하지 않는 사용자입니다.");
    }

    public UserNotFoundException(String email) {
        super("사용자를 찾을 수 없습니다: " + email);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}