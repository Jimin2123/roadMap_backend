package com.shingu.roadmap.security.jwt;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenValidationResult {
    private final boolean valid;
    private final Claims claims;
    private final String errorMessage;
    private final ValidationError errorType;

    public enum ValidationError {
        EXPIRED, MALFORMED, SIGNATURE_INVALID, UNSUPPORTED, GENERAL_ERROR
    }

    // 팩토리 메서드들
    public static TokenValidationResult valid(Claims claims) {
        return new TokenValidationResult(true, claims, null, null);
    }

    public static TokenValidationResult expired() {
        return new TokenValidationResult(false, null, "Token expired", ValidationError.EXPIRED);
    }

    public static TokenValidationResult malformed() {
        return new TokenValidationResult(false, null, "Token malformed", ValidationError.MALFORMED);
    }

    public static TokenValidationResult signatureInvalid() {
        return new TokenValidationResult(false, null, "Token signature invalid", ValidationError.SIGNATURE_INVALID);
    }

    public static TokenValidationResult unsupported() {
        return new TokenValidationResult(false, null, "Token unsupported", ValidationError.UNSUPPORTED);
    }

    public static TokenValidationResult error(String message) {
        return new TokenValidationResult(false, null, message, ValidationError.GENERAL_ERROR);
    }
}