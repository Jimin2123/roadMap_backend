package com.shingu.roadmap.security.event;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public void publishAuthenticationSuccess(String email, String clientIp, String userAgent) {
        AuthenticationEvent event = AuthenticationEvent.success(email, clientIp, userAgent);
        eventPublisher.publishEvent(event);

        meterRegistry.counter("security.authentication.success",
            Tags.of("client_ip", maskIp(clientIp)))
            .increment();

        log.info("Authentication successful for user: {} from IP: {}", email, maskIp(clientIp));
    }

    public void publishAuthenticationFailure(String reason, String clientIp, String userAgent) {
        AuthenticationEvent event = AuthenticationEvent.failure(reason, clientIp, userAgent);
        eventPublisher.publishEvent(event);

        meterRegistry.counter("security.authentication.failure",
            Tags.of("reason", reason, "client_ip", maskIp(clientIp)))
            .increment();

        log.warn("Authentication failed: {} from IP: {}", reason, maskIp(clientIp));
    }

    public void publishAuthenticationError(String clientIp, String userAgent, String errorMessage) {
        AuthenticationEvent event = AuthenticationEvent.error(clientIp, userAgent, errorMessage);
        eventPublisher.publishEvent(event);

        meterRegistry.counter("security.authentication.error",
            Tags.of("client_ip", maskIp(clientIp)))
            .increment();

        log.error("Authentication error from IP: {} - {}", maskIp(clientIp), errorMessage);
    }

    private String maskIp(String ip) {
        if (ip == null || ip.length() < 8) return "unknown";
        return ip.substring(0, ip.lastIndexOf('.')) + ".xxx";
    }
}