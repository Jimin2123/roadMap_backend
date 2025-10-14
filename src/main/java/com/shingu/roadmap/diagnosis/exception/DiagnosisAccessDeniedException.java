package com.shingu.roadmap.diagnosis.exception;

/**
 * 진단 결과 접근 권한이 없는 경우 발생하는 예외
 */
public class DiagnosisAccessDeniedException extends DiagnosisException {

    private static final String ERROR_CODE = "DIAGNOSIS_ACCESS_DENIED";

    public DiagnosisAccessDeniedException(Long diagnosisId, Long memberId) {
        super(ERROR_CODE, String.format(
                "진단 결과에 접근할 권한이 없습니다. diagnosisId: %d, memberId: %d",
                diagnosisId, memberId));
    }

    public DiagnosisAccessDeniedException(String message) {
        super(ERROR_CODE, message);
    }
}
