package com.shingu.roadmap.resume.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ResumeExceptionHandler {

    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResumeNotFoundException(ResumeNotFoundException e) {
        log.error("Resume not found: {}", e.getMessage());
        return createErrorResponse(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidResumeRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidResumeRequestException(InvalidResumeRequestException e) {
        log.error("Invalid resume request: {}", e.getMessage());
        return createErrorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResumeCreationException.class)
    public ResponseEntity<Map<String, Object>> handleResumeCreationException(ResumeCreationException e) {
        log.error("Resume creation failed: {}", e.getMessage());
        return createErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResumeUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleResumeUpdateException(ResumeUpdateException e) {
        log.error("Resume update failed: {}", e.getMessage());
        return createErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPeriodException(InvalidPeriodException e) {
        log.error("Invalid period: {}", e.getMessage());
        return createErrorResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResumeException.class)
    public ResponseEntity<Map<String, Object>> handleResumeException(ResumeException e) {
        log.error("Resume exception: {}", e.getMessage());
        return createErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(ResumeException e, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("code", e.getErrorCode());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("path", "/api/resume");

        return new ResponseEntity<>(errorResponse, status);
    }
}