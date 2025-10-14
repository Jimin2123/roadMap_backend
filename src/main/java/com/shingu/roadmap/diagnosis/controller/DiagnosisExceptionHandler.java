package com.shingu.roadmap.diagnosis.controller;

import com.shingu.roadmap.diagnosis.dto.response.ErrorResponse;
import com.shingu.roadmap.diagnosis.dto.response.ValidationErrorDetail;
import com.shingu.roadmap.diagnosis.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 진단 모듈 전역 예외 처리기
 *
 * <p>모든 진단 관련 예외를 처리하고 클라이언트에게 일관된 에러 응답을 제공합니다.</p>
 *
 * <h3>처리하는 예외 유형</h3>
 * <ul>
 *   <li>DiagnosisNotFoundException - 404 Not Found</li>
 *   <li>DiagnosisAccessDeniedException - 403 Forbidden</li>
 *   <li>DiagnosisAlreadyInProgressException - 409 Conflict</li>
 *   <li>DiagnosisInvalidStateException - 400 Bad Request</li>
 *   <li>DiagnosisTimeoutException - 408 Request Timeout</li>
 *   <li>DiagnosisProcessingException - 500 Internal Server Error</li>
 *   <li>ProfileNotFoundException - 400 Bad Request</li>
 *   <li>ExternalApiException - 502 Bad Gateway</li>
 *   <li>MethodArgumentNotValidException - 400 Bad Request (Validation)</li>
 *   <li>IllegalArgumentException - 400 Bad Request</li>
 *   <li>IllegalStateException - 409 Conflict</li>
 *   <li>Exception (일반) - 500 Internal Server Error</li>
 * </ul>
 */
@RestControllerAdvice(basePackageClasses = DiagnosisController.class)
@Slf4j
public class DiagnosisExceptionHandler {

    /**
     * 진단 결과를 찾을 수 없는 경우
     */
    @ExceptionHandler(DiagnosisNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisNotFoundException(
            DiagnosisNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Diagnosis not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * 진단 결과 접근 권한이 없는 경우
     */
    @ExceptionHandler(DiagnosisAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisAccessDeniedException(
            DiagnosisAccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Diagnosis access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse);
    }

    /**
     * 이미 진행 중인 진단이 있는 경우
     */
    @ExceptionHandler(DiagnosisAlreadyInProgressException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisAlreadyInProgressException(
            DiagnosisAlreadyInProgressException ex,
            HttpServletRequest request
    ) {
        log.warn("Diagnosis already in progress: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * 진단 상태가 유효하지 않은 경우
     */
    @ExceptionHandler(DiagnosisInvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisInvalidStateException(
            DiagnosisInvalidStateException ex,
            HttpServletRequest request
    ) {
        log.warn("Diagnosis invalid state: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 진단 타임아웃 발생 시
     */
    @ExceptionHandler(DiagnosisTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisTimeoutException(
            DiagnosisTimeoutException ex,
            HttpServletRequest request
    ) {
        log.warn("Diagnosis timeout: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.REQUEST_TIMEOUT)
                .body(errorResponse);
    }

    /**
     * 진단 처리 중 오류 발생 시
     */
    @ExceptionHandler(DiagnosisProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisProcessingException(
            DiagnosisProcessingException ex,
            HttpServletRequest request
    ) {
        log.error("Diagnosis processing error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                "진단 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * 프로필을 찾을 수 없는 경우
     */
    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFoundException(
            ProfileNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Profile not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 외부 API 오류 발생 시
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorResponse> handleExternalApiException(
            ExternalApiException ex,
            HttpServletRequest request
    ) {
        log.error("External API error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                "외부 서비스 연동 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(errorResponse);
    }

    /**
     * Bean Validation 실패 시
     * 상세한 필드별 검증 오류 정보를 반환합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Validation failed: {}", ex.getMessage());

        // 모든 필드 에러를 수집
        List<ValidationErrorDetail> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ValidationErrorDetail.of(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        // 첫 번째 에러 메시지를 대표 메시지로 사용
        String mainMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("입력값이 유효하지 않습니다.");

        ErrorResponse errorResponse = ErrorResponse.ofValidation(
                "VALIDATION_FAILED",
                mainMessage,
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * IllegalArgumentException 처리
     * 잘못된 인자가 전달된 경우 (예: Member not found, Profile not found 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * IllegalStateException 처리
     * 잘못된 상태에서 작업이 시도된 경우
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        log.warn("Illegal state: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                "INVALID_STATE",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * 기타 진단 관련 예외
     * 위에서 처리되지 않은 DiagnosisException 하위 클래스 처리
     */
    @ExceptionHandler(DiagnosisException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisException(
            DiagnosisException ex,
            HttpServletRequest request
    ) {
        log.error("Diagnosis error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * 처리되지 않은 모든 예외에 대한 폴백 핸들러
     *
     * <p>예상하지 못한 예외가 발생한 경우 사용자에게 일반적인 에러 메시지를 반환하고
     * 서버 로그에 상세 정보를 기록합니다.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error occurred in diagnosis module", ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}
