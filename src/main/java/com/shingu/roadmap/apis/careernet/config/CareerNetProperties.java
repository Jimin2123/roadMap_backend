package com.shingu.roadmap.apis.careernet.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "career-net")
@Data
public class CareerNetProperties {
  @NotBlank
  String baseUrl;

  @NotBlank
  String apiKey;
}
