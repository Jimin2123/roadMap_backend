package com.shingu.roadmap.apis.work24.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "work24")
@Data
public class Work24Properties {

  @NotBlank
  String traningCourceUrl;

  @NotBlank
  String traningCourceKey;
}
