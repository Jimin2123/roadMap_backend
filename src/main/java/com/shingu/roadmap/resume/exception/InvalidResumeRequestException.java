package com.shingu.roadmap.resume.exception;

public class InvalidResumeRequestException extends ResumeException {

    public InvalidResumeRequestException(String message) {
        super(message);
    }

    public InvalidResumeRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return ResumeErrorCode.RESUME_002.getCode();
    }
}