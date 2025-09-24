package com.shingu.roadmap.resume.exception;

public class ResumeUpdateException extends ResumeException {

    public ResumeUpdateException(String message) {
        super(message);
    }

    public ResumeUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ResumeErrorCode.RESUME_004.getCode();
    }
}