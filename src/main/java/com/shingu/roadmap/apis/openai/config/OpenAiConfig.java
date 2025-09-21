package com.shingu.roadmap.apis.openai.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Slf4j
@Validated
@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAiConfig {

  @NotBlank
  private String apiKey;

  @NotBlank
  private String baseUrl = "https://api.openai.com";

  @NotBlank
  private String model = "gpt-4-turbo-preview";

  @NotBlank
  private String ncsCodeAssistantId;

  @Min(0) @Max(1)
  private double temperature = 0.1;

  // 타임아웃 설정
  private Duration connectTimeout = Duration.ofSeconds(10);
  private Duration readTimeout = Duration.ofSeconds(45);
  private Duration writeTimeout = Duration.ofSeconds(30);

  // 재시도 설정
  private int maxRetryAttempts = 3;
  private Duration initialRetryDelay = Duration.ofSeconds(1);
  private Duration maxRetryDelay = Duration.ofSeconds(60);

  // 연결 풀 설정
  private int maxConnections = 50;
  private Duration maxIdleTime = Duration.ofSeconds(30);
  private Duration maxLifeTime = Duration.ofMinutes(5);

  // 캐시 설정
  private Duration threadCacheTtl = Duration.ofHours(2);
  private Duration responseCacheTtl = Duration.ofHours(1);
  private int maxCacheEntries = 1000;

  // 모니터링 설정
  private boolean monitoringEnabled = true;
  private Duration slowRequestThreshold = Duration.ofSeconds(10);
  private double errorRateThreshold = 0.05;

  @PostConstruct
  private void validateConfiguration() {
    if (!StringUtils.hasText(apiKey)) {
      throw new IllegalStateException("OpenAI API key must be configured");
    }

    if (apiKey.length() < 20) {
      throw new IllegalStateException("Invalid OpenAI API key format");
    }

    if (!apiKey.startsWith("sk-")) {
      log.warn("OpenAI API key should start with 'sk-' prefix");
    }

    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalStateException("OpenAI base URL must be configured");
    }

    if (!StringUtils.hasText(ncsCodeAssistantId)) {
      throw new IllegalStateException("OpenAI assistant ID must be configured");
    }

    log.info("OpenAI configuration validated successfully - model: {}, baseUrl: {}",
             model, baseUrl);
  }

  public String getSecureApiKey() {
    return apiKey;
  }

  public String getMaskedApiKey() {
    if (!StringUtils.hasText(apiKey) || apiKey.length() <= 8) {
      return "****";
    }
    return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
  }
}
