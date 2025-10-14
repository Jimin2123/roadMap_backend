package com.shingu.roadmap.diagnosis.exception;

/**
 * 진단 처리 중 예외가 발생한 경우
 */
public class DiagnosisProcessingException extends DiagnosisException {

    public DiagnosisProcessingException(String message) {
        super("DIAGNOSIS_PROCESSING_ERROR", message);
    }

    public DiagnosisProcessingException(String message, Throwable cause) {
        super("DIAGNOSIS_PROCESSING_ERROR", message, cause);
    }

    public DiagnosisProcessingException(Long diagnosisId, String processorName, Throwable cause) {
        super(
                "DIAGNOSIS_PROCESSING_ERROR",
                String.format("진단 처리 중 오류가 발생했습니다. diagnosisId: %d, processor: %s", diagnosisId, processorName),
                cause
        );
    }
}
