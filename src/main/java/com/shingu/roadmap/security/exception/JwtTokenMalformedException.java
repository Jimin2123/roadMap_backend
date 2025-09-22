package com.shingu.roadmap.security.exception;

public class JwtTokenMalformedException extends SecurityException {

    private static final String ERROR_CODE = "SEC_002";

    public JwtTokenMalformedException() {
        super("Token is malformed");
    }

    public JwtTokenMalformedException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}