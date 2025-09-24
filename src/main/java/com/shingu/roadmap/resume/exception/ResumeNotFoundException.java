package com.shingu.roadmap.resume.exception;

public class ResumeNotFoundException extends ResumeException {

    public ResumeNotFoundException(String message) {
        super(message);
    }

    public ResumeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ResumeErrorCode.RESUME_001.getCode();
    }
}