package com.shingu.roadmap.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

  private final JwtProperties jwtProperties;

  public JwtUtil(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String generateToken(String tokenType, TokenPayload payload) {

    JwtProperties.TokenProperties tokenProperties =
            "access".equals(tokenType) ? jwtProperties.getAccessToken() : jwtProperties.getRefreshToken();

    Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(tokenProperties.getSecretKey()));

    Claims claims = Jwts.claims();
    claims.put("memberId", payload.memberId());
    claims.put("email", payload.email());
    claims.put("name", payload.name());
    claims.put("role", payload.role()); //USER, ADMIN

    return Jwts.builder()
            .setClaims(claims)
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

  public Claims parseClaims(String tokenType,String token) {
    String secretKey = "access".equals(tokenType)
            ? jwtProperties.getAccessToken().getSecretKey()
            : jwtProperties.getRefreshToken().getSecretKey();

    Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

    return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
  }
}
