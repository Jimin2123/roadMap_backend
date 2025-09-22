package com.shingu.roadmap.security.exception;

public class JwtTokenExpiredException extends SecurityException {

    private static final String ERROR_CODE = "SEC_001";

    public JwtTokenExpiredException(String tokenType) {
        super(tokenType + " token has expired");
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}