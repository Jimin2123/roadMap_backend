package com.shingu.roadmap.security.event;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityEventPublisher {

    private static final String METRIC_AUTH_SUCCESS = "security.authentication.success";
    private static final String METRIC_AUTH_FAILURE = "security.authentication.failure";
    private static final String METRIC_AUTH_ERROR   = "security.authentication.error";

    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public void publishAuthenticationSuccess(String email, String clientIp, String userAgent) {
        safeRun(() -> {
            AuthenticationEvent event = AuthenticationEvent.success(email, clientIp, userAgent);
            eventPublisher.publishEvent(event);

            Tags tags = Tags.of(
                    "client_ip", maskIp(clientIp),
                    "ua", shortenUA(userAgent)
            );
            meterRegistry.counter(METRIC_AUTH_SUCCESS, tags).increment();

            log.info("AUTH SUCCESS user={} ip={} ua={}",
                    safe(email), maskIp(clientIp), shortenUA(userAgent));
        });
    }

    public void publishAuthenticationFailure(String reason, String clientIp, String userAgent) {
        safeRun(() -> {
            AuthenticationEvent event = AuthenticationEvent.failure(reason, clientIp, userAgent);
            eventPublisher.publishEvent(event);

            Tags tags = Tags.of(
                    "reason", safeReason(reason),
                    "client_ip", maskIp(clientIp),
                    "ua", shortenUA(userAgent)
            );
            meterRegistry.counter(METRIC_AUTH_FAILURE, tags).increment();

            log.warn("AUTH FAILURE reason={} ip={} ua={}",
                    safeReason(reason), maskIp(clientIp), shortenUA(userAgent));
        });
    }

    public void publishAuthenticationError(String clientIp, String userAgent, String errorMessage) {
        safeRun(() -> {
            AuthenticationEvent event = AuthenticationEvent.error(clientIp, userAgent, errorMessage);
            eventPublisher.publishEvent(event);

            Tags tags = Tags.of(
                    "client_ip", maskIp(clientIp),
                    "ua", shortenUA(userAgent)
            );
            meterRegistry.counter(METRIC_AUTH_ERROR, tags).increment();

            log.error("AUTH ERROR ip={} ua={} msg={}",
                    maskIp(clientIp), shortenUA(userAgent), safe(errorMessage));
        });
    }

    /** ===================== 유틸들 ===================== */

    /**
     * IPv4/IPv6/기타 입력 모두 예외 없이 마스킹.
     * - IPv4: a.b.c.d  → a.b.c.*   (또는 a.b.c.0/24로 바꾸고 싶으면 주석 변경)
     * - IPv6: 2001:db8:...:7348  → 2001:db8:*:7348
     * - ::1 / 0:0:0:0:0:0:0:1 → ::1
     * - null/빈값/이상치 → "unknown"
     */
    String maskIp(String ip) {
        if (ip == null) return "unknown";
        ip = ip.trim();
        if (ip.isEmpty()) return "unknown";

        // IPv6 계열 (':' 포함)
        if (ip.indexOf(':') >= 0) {
            // 루프백 특례
            if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return "::1";
            try {
                InetAddress addr = InetAddress.getByName(ip);
                if (addr instanceof Inet6Address) {
                    // hextet 2개 + * + 마지막 hextet 노출
                    String[] raw = ip.split(":");
                    String[] parts = Arrays.stream(raw).filter(s -> !s.isEmpty()).toArray(String[]::new);
                    if (parts.length == 0) return "::";
                    String first = parts[0];
                    String second = parts.length > 1 ? parts[1] : "";
                    String last = parts[parts.length - 1];
                    if (!second.isEmpty()) {
                        return first + ":" + second + ":*:" + last;
                    } else {
                        return first + ":*:" + last;
                    }
                }
            } catch (Exception ignore) {
                // 파싱 실패 시에도 안전 문자열 반환
            }
            // 파싱 실패 등 애매한 IPv6 문자열
            return ip.replaceAll("([0-9a-fA-F]{1,4})(:[0-9a-fA-F]{0,4}){1,}", "$1:*");
        }

        // IPv4 계열 ('.' 포함)
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".*"; // 또는 + ".0/24"
        }

        // 그 외
        return "unknown";
    }

    /** 메트릭 태그 카디널리티 폭증 방지: 너무 긴 UA는 자름 */
    private String shortenUA(String ua) {
        if (ua == null || ua.isBlank()) return "unknown";
        String s = ua.trim();
        // 필요시 대표 토큰만 남기는 정규식/파서로 더 줄여도 됨
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    /** reason 태그 정리 (null/빈값 방지 & 길이 제한) */
    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) return "unspecified";
        String r = reason.trim();
        return r.length() > 64 ? r.substring(0, 64) + "…" : r;
    }

    /** 로그/태그용 안전 문자열 */
    private String safe(String s) {
        if (s == null || s.isBlank()) return "unknown";
        return s.trim();
    }

    /** 퍼블리시/메트릭 실패가 요청 흐름을 깨지 않도록 가드 */
    private void safeRun(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            // 여기서 절대 예외 전파하지 않음
            log.warn("security event publish failed: {}", e.toString());
        }
    }
}
