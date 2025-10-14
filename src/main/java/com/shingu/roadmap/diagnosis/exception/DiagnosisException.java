package com.shingu.roadmap.diagnosis.exception;

/**
 * 진단 모듈의 기본 예외 클래스
 * 모든 진단 관련 예외는 이 클래스를 상속합니다.
 */
public class DiagnosisException extends RuntimeException {

    private final String errorCode;

    public DiagnosisException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DiagnosisException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
