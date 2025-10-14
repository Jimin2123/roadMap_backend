package com.shingu.roadmap.diagnosis.exception;

/**
 * 진단 처리 시간이 초과된 경우 발생하는 예외
 */
public class DiagnosisTimeoutException extends DiagnosisException {

    public DiagnosisTimeoutException(Long diagnosisId, long timeoutSeconds) {
        super(
                "DIAGNOSIS_TIMEOUT",
                String.format("진단 처리 시간이 초과되었습니다. diagnosisId: %d, timeout: %d초", diagnosisId, timeoutSeconds)
        );
    }

    public DiagnosisTimeoutException(String message) {
        super("DIAGNOSIS_TIMEOUT", message);
    }
}
