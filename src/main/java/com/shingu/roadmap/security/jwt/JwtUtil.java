package com.shingu.roadmap.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

  private final JwtProperties jwtProperties;

  public JwtUtil(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String generateToken(String tokenType, TokenPayload payload) {
    JwtProperties.TokenProperties tokenProperties =
            "access".equals(tokenType) ? jwtProperties.getAccess() : jwtProperties.getRefresh();

    Key key = Keys.hmacShaKeyFor(tokenProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

    Claims claims = Jwts.claims();
    claims.put("memberId", payload.memberId());
    claims.put("email", payload.email());
    claims.put("name", payload.name());
    claims.put("role", payload.role()); //USER, ADMIN

    return Jwts.builder()
            .setClaims(claims)
            .setId(UUID.randomUUID().toString()) // JTI (JWT ID) 클레임 설정으로 토큰 고유성 보장
            .setSubject(payload.memberId().toString())
            .setIssuer(jwtProperties.getIssuer())
            .setAudience(jwtProperties.getAudience())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + tokenProperties.getExpiration()))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
  }

  public String generateAccessToken(TokenPayload payload) {
    return generateToken("access", payload);
  }

  public String generateRefreshToken(TokenPayload payload) {
    return generateToken("refresh", payload);
  }

  public String verifyAccessToken(String accessToken) {
    return parseClaims("access", accessToken).getSubject();
  }

  public String verifyRefreshToken(String refreshToken) {
    return parseClaims("refresh", refreshToken).getSubject();
  }

  public boolean isValidRefreshToken(String refreshToken) {
    try {
      verifyRefreshToken(refreshToken);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public TokenValidationResult validateToken(String token, TokenType tokenType) {
    try {
      Claims claims = parseClaimsInternal(token, tokenType);

      // Audience 검증
      if (!jwtProperties.getAudience().equals(claims.getAudience())) {
        return TokenValidationResult.error("Invalid audience");
      }

      return TokenValidationResult.valid(claims);

    } catch (ExpiredJwtException e) {
      log.warn("Token expired: {}", e.getMessage());
      return TokenValidationResult.expired();

    } catch (MalformedJwtException e) {
      log.warn("Malformed token: {}", e.getMessage());
      return TokenValidationResult.malformed();

    } catch (SignatureException e) {
      log.error("Token signature verification failed: {}", e.getMessage());
      return TokenValidationResult.signatureInvalid();

    } catch (UnsupportedJwtException e) {
      log.warn("Unsupported token: {}", e.getMessage());
      return TokenValidationResult.unsupported();

    } catch (Exception e) {
      log.error("Unexpected error during token validation: {}", e.getMessage(), e);
      return TokenValidationResult.error(e.getMessage());
    }
  }

  public Claims parseClaims(String tokenType, String token) {
    try {
      return parseClaimsInternal(token, TokenType.valueOf(tokenType.toUpperCase()));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid token type: " + tokenType, e);
    }
  }

  private Claims parseClaimsInternal(String token, TokenType tokenType) {
    String secretKey = TokenType.ACCESS.equals(tokenType)
            ? jwtProperties.getAccess().getSecretKey()
            : jwtProperties.getRefresh().getSecretKey();

    Key key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

    return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
  }
}
