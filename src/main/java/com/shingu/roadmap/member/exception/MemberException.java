package com.shingu.roadmap.member.exception;

public abstract class MemberException extends RuntimeException {

    protected MemberException(String message) {
        super(message);
    }

    protected MemberException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getErrorCode();
}