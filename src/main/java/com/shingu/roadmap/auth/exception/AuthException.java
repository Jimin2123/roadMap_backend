package com.shingu.roadmap.auth.exception;

public abstract class AuthException extends RuntimeException {

    protected AuthException(String message) {
        super(message);
    }

    protected AuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getErrorCode();
}