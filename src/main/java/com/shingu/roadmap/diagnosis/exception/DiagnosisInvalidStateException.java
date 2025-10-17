package com.shingu.roadmap.diagnosis.exception;

/**
 * 진단 상태가 유효하지 않은 경우 발생하는 예외
 */
public class DiagnosisInvalidStateException extends DiagnosisException {

    private static final String ERROR_CODE = "DIAGNOSIS_INVALID_STATE";

    public DiagnosisInvalidStateException(String message) {
        super(ERROR_CODE, message);
    }

    public DiagnosisInvalidStateException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
