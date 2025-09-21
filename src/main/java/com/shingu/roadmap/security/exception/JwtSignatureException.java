package com.shingu.roadmap.security.exception;

public class JwtSignatureException extends SecurityException {

    private static final String ERROR_CODE = "SEC_003";

    public JwtSignatureException() {
        super("Token signature verification failed");
    }

    public JwtSignatureException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}