package com.shingu.roadmap.apis.openai.notification;

import com.shingu.roadmap.apis.openai.config.OpenAiConfig;
import com.shingu.roadmap.apis.openai.logging.SecureLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiNotificationService {

    private final OpenAiConfig config;
    private final SecureLogger secureLogger;
    private final RestTemplate restTemplate = new RestTemplate();

    // 알림 중복 방지를 위한 캐시 (알림 타입별 마지막 발송 시간)
    private final ConcurrentHashMap<String, LocalDateTime> lastNotificationTime = new ConcurrentHashMap<>();

    // 알림 억제 설정 (동일한 알림 타입에 대한 최소 간격)
    private static final Map<NotificationType, Integer> NOTIFICATION_INTERVALS = Map.of(
        NotificationType.CRITICAL_ERROR, 5,     // 5분
        NotificationType.HIGH_ERROR_RATE, 15,   // 15분
        NotificationType.SLOW_RESPONSE, 10,     // 10분
        NotificationType.QUOTA_EXCEEDED, 30,    // 30분
        NotificationType.CONNECTION_POOL_WARN, 20, // 20분
        NotificationType.SERVICE_DOWN, 1        // 1분 (긴급)
    );

    /**
     * 이벤트 기반 알림 처리
     */
    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            if (shouldSendNotification(event.getType())) {
                sendNotification(event);
                updateLastNotificationTime(event.getType());
                secureLogger.logSecurityEvent("notification", "SENT",
                    String.format("Type: %s, Severity: %s", event.getType(), event.getSeverity()));
            } else {
                secureLogger.logSecurityEvent("notification", "SUPPRESSED",
                    String.format("Type: %s (too frequent)", event.getType()));
            }
        } catch (Exception e) {
            secureLogger.logApiError("notification", "handleNotificationEvent",
                                   "NOTIFICATION_ERROR", e.getMessage());
        }
    }

    /**
     * 직접 알림 발송
     */
    public void sendCriticalAlert(String title, String message, String source) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.CRITICAL_ERROR)
                .severity(NotificationSeverity.CRITICAL)
                .title(title)
                .message(message)
                .source(source)
                .timestamp(LocalDateTime.now())
                .build();

        handleNotificationEvent(event);
    }

    /**
     * 에러율 경고 알림
     */
    public void sendErrorRateAlert(double errorRate, double threshold) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.HIGH_ERROR_RATE)
                .severity(NotificationSeverity.WARNING)
                .title("🚨 OpenAI API 에러율 임계값 초과")
                .message(String.format("현재 에러율: %.2f%% (임계값: %.2f%%)", errorRate, threshold))
                .source("OpenAI-ErrorRateMonitor")
                .timestamp(LocalDateTime.now())
                .additionalData(Map.of(
                    "current_error_rate", errorRate,
                    "threshold", threshold,
                    "action_required", "에러 원인 조사 및 대응 필요"
                ))
                .build();

        handleNotificationEvent(event);
    }

    /**
     * 응답 시간 경고 알림
     */
    public void sendSlowResponseAlert(double avgResponseTime, long threshold) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.SLOW_RESPONSE)
                .severity(NotificationSeverity.WARNING)
                .title("⏰ OpenAI API 응답 시간 지연")
                .message(String.format("평균 응답 시간: %.2fms (임계값: %dms)", avgResponseTime, threshold))
                .source("OpenAI-ResponseTimeMonitor")
                .timestamp(LocalDateTime.now())
                .additionalData(Map.of(
                    "avg_response_time", avgResponseTime,
                    "threshold", threshold,
                    "recommendation", "연결 상태 및 서버 부하 확인 필요"
                ))
                .build();

        handleNotificationEvent(event);
    }

    /**
     * 할당량 초과 알림
     */
    public void sendQuotaExceededAlert(String quotaType) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.QUOTA_EXCEEDED)
                .severity(NotificationSeverity.CRITICAL)
                .title("❌ OpenAI API 할당량 초과")
                .message(String.format("할당량 타입: %s - 즉시 대응 필요", quotaType))
                .source("OpenAI-QuotaMonitor")
                .timestamp(LocalDateTime.now())
                .additionalData(Map.of(
                    "quota_type", quotaType,
                    "action_required", "요금제 확인 및 업그레이드 검토",
                    "impact", "서비스 중단 가능성"
                ))
                .build();

        handleNotificationEvent(event);
    }

    /**
     * 연결 풀 경고 알림
     */
    public void sendConnectionPoolAlert(double utilization, int maxConnections) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.CONNECTION_POOL_WARN)
                .severity(NotificationSeverity.WARNING)
                .title("🔗 OpenAI 연결 풀 사용률 높음")
                .message(String.format("연결 풀 사용률: %.2f%% (최대: %d개)", utilization * 100, maxConnections))
                .source("OpenAI-ConnectionPoolMonitor")
                .timestamp(LocalDateTime.now())
                .additionalData(Map.of(
                    "utilization", utilization,
                    "max_connections", maxConnections,
                    "recommendation", "연결 풀 크기 증가 또는 트래픽 분산 고려"
                ))
                .build();

        handleNotificationEvent(event);
    }

    /**
     * 알림 발송 여부 결정
     */
    private boolean shouldSendNotification(NotificationType type) {
        if (!config.isMonitoringEnabled()) {
            return false;
        }

        LocalDateTime lastTime = lastNotificationTime.get(type.name());
        if (lastTime == null) {
            return true;
        }

        int intervalMinutes = NOTIFICATION_INTERVALS.getOrDefault(type, 30);
        return lastTime.plusMinutes(intervalMinutes).isBefore(LocalDateTime.now());
    }

    /**
     * 실제 알림 발송
     */
    private void sendNotification(NotificationEvent event) {
        try {
            // 1. Slack 알림 발송
            sendSlackNotification(event);

            // 2. 이메일 알림 발송 (중요한 경우)
            if (event.getSeverity() == NotificationSeverity.CRITICAL) {
                sendEmailNotification(event);
            }

            // 3. 시스템 로그 기록
            logNotification(event);

        } catch (Exception e) {
            secureLogger.logApiError("notification", "sendNotification",
                                   "SEND_FAILED", e.getMessage());
        }
    }

    /**
     * Slack 알림 발송
     */
    private void sendSlackNotification(NotificationEvent event) {
        try {
            // 실제 구현에서는 Slack Webhook URL 사용
            String webhookUrl = getSlackWebhookUrl();
            if (webhookUrl == null) {
                log.debug("Slack webhook URL not configured, skipping Slack notification");
                return;
            }

            SlackAttachment attachment = SlackAttachment.builder()
                    .color(getSeverityColor(event.getSeverity()))
                    .title(event.getTitle())
                    .text(event.getMessage())
                    .build();

            attachment.addField("Source", event.getSource(), true);
            attachment.addField("Timestamp",
                    event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), true);
            attachment.addField("Severity", event.getSeverity().name(), true);

            SlackMessage slackMessage = SlackMessage.builder()
                    .text(event.getTitle())
                    .username("OpenAI-Monitor")
                    .iconEmoji(getSeverityEmoji(event.getSeverity()))
                    .attachments(List.of(attachment))
                    .build();

            restTemplate.postForEntity(webhookUrl, slackMessage, String.class);

        } catch (Exception e) {
            log.error("Failed to send Slack notification", e);
        }
    }

    /**
     * 이메일 알림 발송
     */
    private void sendEmailNotification(NotificationEvent event) {
        try {
            // 실제 구현에서는 EmailService 사용
            log.info("Critical alert email would be sent: {}", event.getTitle());

        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }
    }

    /**
     * 알림 로그 기록
     */
    private void logNotification(NotificationEvent event) {
        secureLogger.logSecurityEvent("notification", "ALERT_SENT",
                String.format("Title: %s, Severity: %s, Source: %s",
                             event.getTitle(), event.getSeverity(), event.getSource()));
    }

    /**
     * 마지막 알림 시간 업데이트
     */
    private void updateLastNotificationTime(NotificationType type) {
        lastNotificationTime.put(type.name(), LocalDateTime.now());
    }

    /**
     * 심각도별 이모지 반환
     */
    private String getSeverityEmoji(NotificationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> ":red_circle:";
            case WARNING -> ":warning:";
            case INFO -> ":information_source:";
        };
    }

    /**
     * 심각도별 색상 반환
     */
    private String getSeverityColor(NotificationSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "#FF0000";
            case WARNING -> "#FFA500";
            case INFO -> "#0000FF";
        };
    }

    /**
     * Slack Webhook URL 조회 (환경변수에서)
     */
    private String getSlackWebhookUrl() {
        return System.getenv("SLACK_WEBHOOK_URL");
    }

    /**
     * 알림 통계 조회
     */
    public NotificationStats getNotificationStats() {
        return NotificationStats.builder()
                .totalNotificationTypes(NOTIFICATION_INTERVALS.size())
                .lastNotificationTimes(new HashMap<>(lastNotificationTime))
                .build();
    }

    // DTO 클래스들
    @lombok.Builder
    @lombok.Data
    public static class NotificationEvent {
        private NotificationType type;
        private NotificationSeverity severity;
        private String title;
        private String message;
        private String source;
        private LocalDateTime timestamp;
        private Map<String, Object> additionalData;
    }

    @lombok.Builder
    @lombok.Data
    public static class SlackMessage {
        private String text;
        private String username;
        private String iconEmoji;
        private List<SlackAttachment> attachments;

        public SlackMessage attachment(SlackAttachment attachment) {
            if (this.attachments == null) {
                this.attachments = new ArrayList<>();
            }
            this.attachments.add(attachment);
            return this;
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class SlackAttachment {
        private String color;
        private String title;
        private String text;
        private List<SlackField> fields;

        public SlackAttachment addField(String title, String value, boolean shortField) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            this.fields.add(new SlackField(title, value, shortField));
            return this;
        }
    }

    @lombok.AllArgsConstructor
    @lombok.Data
    public static class SlackField {
        private String title;
        private String value;
        private boolean shortValue;
    }

    @lombok.Builder
    @lombok.Data
    public static class NotificationStats {
        private int totalNotificationTypes;
        private Map<String, LocalDateTime> lastNotificationTimes;
    }

    public enum NotificationType {
        CRITICAL_ERROR,
        HIGH_ERROR_RATE,
        SLOW_RESPONSE,
        QUOTA_EXCEEDED,
        CONNECTION_POOL_WARN,
        SERVICE_DOWN,
        CACHE_FAILURE,
        SECURITY_ALERT
    }

    public enum NotificationSeverity {
        CRITICAL,
        WARNING,
        INFO
    }
}