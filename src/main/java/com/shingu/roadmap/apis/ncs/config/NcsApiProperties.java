package com.shingu.roadmap.apis.ncs.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "ncs")
@Data
public class NcsApiProperties {

  @NotBlank
  private String baseUrl;

  @NotBlank
  private String serviceKey;
}
