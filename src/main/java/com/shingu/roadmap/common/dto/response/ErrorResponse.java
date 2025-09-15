package com.shingu.roadmap.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shingu.roadmap.common.exception.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // Null인 필드는 JSON에서 제외
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String code;
    private final String message;

    public ErrorResponse(ErrorCode errorCode, String customMessage) {
        this.status = errorCode.getStatus().value();
        this.error = errorCode.getStatus().getReasonPhrase();
        this.code = errorCode.name();
        this.message = (customMessage != null) ? customMessage : errorCode.getMessage();
    }

    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }
}
