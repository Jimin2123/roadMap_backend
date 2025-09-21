package com.shingu.roadmap.security.exception;

public class JwtUnsupportedTokenException extends SecurityException {

    private static final String ERROR_CODE = "SEC_004";

    public JwtUnsupportedTokenException() {
        super("Unsupported token type");
    }

    public JwtUnsupportedTokenException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}