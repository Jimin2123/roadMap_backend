package com.shingu.roadmap.diagnosis.exception;

/**
 * 외부 API 호출 중 오류가 발생한 경우
 */
public class ExternalApiException extends DiagnosisException {

    public ExternalApiException(String apiName, String message) {
        super(
                "EXTERNAL_API_ERROR",
                String.format("외부 API 호출 중 오류가 발생했습니다. API: %s, 메시지: %s", apiName, message)
        );
    }

    public ExternalApiException(String apiName, Throwable cause) {
        super(
                "EXTERNAL_API_ERROR",
                String.format("외부 API 호출 중 오류가 발생했습니다. API: %s", apiName),
                cause
        );
    }
}
