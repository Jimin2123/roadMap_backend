package com.shingu.roadmap.resume.exception;

public class InvalidPeriodException extends ResumeException {

    public InvalidPeriodException(String message) {
        super(message);
    }

    public InvalidPeriodException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ResumeErrorCode.RESUME_009.getCode();
    }
}