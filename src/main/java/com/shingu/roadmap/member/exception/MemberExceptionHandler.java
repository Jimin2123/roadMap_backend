package com.shingu.roadmap.member.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class MemberExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMemberNotFoundException(MemberNotFoundException ex) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProfileNotFoundException(ProfileNotFoundException ex) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateMemberException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMemberException(DuplicateMemberException ex) {
        return createErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CertificateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCertificateNotFoundException(CertificateNotFoundException ex) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(JobCodeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleJobCodeNotFoundException(JobCodeNotFoundException ex) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MemberUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleMemberUpdateException(MemberUpdateException ex) {
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProfileCreationException.class)
    public ResponseEntity<Map<String, Object>> handleProfileCreationException(ProfileCreationException ex) {
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberException.class)
    public ResponseEntity<Map<String, Object>> handleMemberException(MemberException ex) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        // 이메일 중복 오류인지 확인
        if (ex.getMessage() != null && ex.getMessage().contains("idx_account_email")) {
            DuplicateMemberException duplicateEx = new DuplicateMemberException("이미 존재하는 이메일입니다.");
            return createErrorResponse(duplicateEx, HttpStatus.CONFLICT);
        }

        // 기타 데이터 무결성 오류
        MemberUpdateException updateEx = new MemberUpdateException("데이터 무결성 위반 오류가 발생했습니다.");
        return createErrorResponse(updateEx, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(MemberException ex, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(status).body(errorResponse);
    }
}