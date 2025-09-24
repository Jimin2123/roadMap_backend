package com.shingu.roadmap.resume.exception;

public class ResumeCreationException extends ResumeException {

    public ResumeCreationException(String message) {
        super(message);
    }

    public ResumeCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ResumeErrorCode.RESUME_003.getCode();
    }
}