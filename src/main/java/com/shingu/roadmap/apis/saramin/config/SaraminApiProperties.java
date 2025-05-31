package com.shingu.roadmap.apis.saramin.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "saramin")
@Data
public class SaraminApiProperties {

  @NotBlank
  private String baseUrl;

  @NotBlank
  private String apiKey;
}
