package com.shingu.roadmap.diagnosis.exception;

/**
 * 이미 진행 중인 진단이 있는 경우 발생하는 예외
 */
public class DiagnosisAlreadyInProgressException extends DiagnosisException {

    private static final String ERROR_CODE = "DIAGNOSIS_ALREADY_IN_PROGRESS";

    public DiagnosisAlreadyInProgressException(Long memberId) {
        super(ERROR_CODE, String.format(
                "이미 진행 중인 진단이 있습니다. memberId: %d", memberId));
    }

    public DiagnosisAlreadyInProgressException(String message) {
        super(ERROR_CODE, message);
    }
}
