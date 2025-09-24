package com.shingu.roadmap.resume.exception;

public abstract class ResumeException extends RuntimeException {

    protected ResumeException(String message) {
        super(message);
    }

    protected ResumeException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getErrorCode();
}