package com.shingu.roadmap.apis.openai.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAiConfig {

  @NotBlank
  private String apiKey;

  @NotBlank
  private String baseUrl;

  @NotBlank
  private String model;

  @NotBlank
  private String ncsCodeAssistantId;

  @Min(0) @Max(1)
  private double temperature;
}
