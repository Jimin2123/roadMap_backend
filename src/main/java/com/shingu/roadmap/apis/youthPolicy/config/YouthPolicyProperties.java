package com.shingu.roadmap.apis.youthPolicy.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "youth-policy")
@Data
public class YouthPolicyProperties {

  @NotBlank
  private String baseUrl;

  @NotBlank
  private String apiKey;

  // API 키 발급 받으면 지워야 함
  @NotBlank
  private String cookie;
}
