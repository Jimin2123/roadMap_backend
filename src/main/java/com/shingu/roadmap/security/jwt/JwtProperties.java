package com.shingu.roadmap.security.jwt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

  @NotBlank
  private String secretKey;

  @NotBlank
  private String issuer;

  @Valid
  private TokenProperties accessToken;

  @Valid
  private TokenProperties refreshToken;

  @NotBlank
  private String audience;

  @Getter
  @Setter
  public static class TokenProperties {
    @NotBlank
    private String secretKey;

    @NotNull
    private Long expiration;
  }
}
