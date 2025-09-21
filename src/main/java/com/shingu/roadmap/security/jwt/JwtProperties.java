package com.shingu.roadmap.security.jwt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
  private String issuer;

  @NotBlank
  private String audience;

  @Valid
  @NotNull
  private TokenProperties access;

  @Valid
  @NotNull
  private TokenProperties refresh;

  @Getter
  @Setter
  public static class TokenProperties {
    @NotBlank
    @Size(min = 32, message = "Secret key must be at least 32 characters")
    private String secretKey;

    @NotNull
    @Min(value = 60000, message = "Expiration must be at least 1 minute")
    private Long expiration;

    @NotBlank
    private String algorithm = "HS256";
  }

  // Deprecated: 호환성을 위해 유지, 실제로는 access 사용
  @Deprecated
  public TokenProperties getAccessToken() {
    return access;
  }

  // Deprecated: 호환성을 위해 유지, 실제로는 refresh 사용
  @Deprecated
  public TokenProperties getRefreshToken() {
    return refresh;
  }
}
