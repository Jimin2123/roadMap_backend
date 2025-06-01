package com.shingu.roadmap.apis.qnet.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "qnet")
@Data
public class QnetProperties {

  @NotBlank
  String baseUrl;

  @NotBlank
  String serviceKey;
}
