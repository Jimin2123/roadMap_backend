package com.shingu.roadmap.diagnosis.exception;

/**
 * 진단 결과를 찾을 수 없는 경우 발생하는 예외
 */
public class DiagnosisNotFoundException extends DiagnosisException {

    private static final String ERROR_CODE = "DIAGNOSIS_NOT_FOUND";

    public DiagnosisNotFoundException(Long diagnosisId) {
        super(ERROR_CODE, String.format("진단 결과를 찾을 수 없습니다. diagnosisId: %d", diagnosisId));
    }

    public DiagnosisNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
