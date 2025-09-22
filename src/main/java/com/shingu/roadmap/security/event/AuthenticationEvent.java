package com.shingu.roadmap.security.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuthenticationEvent {
    private final EventType type;
    private final String email;
    private final String clientIp;
    private final String userAgent;
    private final String reason;
    private final String errorMessage;
    private final LocalDateTime timestamp;

    public enum EventType {
        SUCCESS, FAILURE, ERROR
    }

    public static AuthenticationEvent success(String email, String clientIp, String userAgent) {
        return AuthenticationEvent.builder()
            .type(EventType.SUCCESS)
            .email(email)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static AuthenticationEvent failure(String reason, String clientIp, String userAgent) {
        return AuthenticationEvent.builder()
            .type(EventType.FAILURE)
            .reason(reason)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static AuthenticationEvent error(String clientIp, String userAgent, String errorMessage) {
        return AuthenticationEvent.builder()
            .type(EventType.ERROR)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }
}