package com.shingu.roadmap.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "refresh_token",
        indexes = { @Index(name = "idx_refresh_token_expires_at", columnList = "expiresAt") }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // for @Builder
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 토큰 문자열은 길이가 길 수 있어 TEXT 사용 */
  @Column(nullable = false, unique = true, columnDefinition = "TEXT")
  private String token;

  @Column(nullable = false)
  private Instant expiresAt;

  /* ===== 비즈니스 메서드 ===== */

  /** 토큰 문자열 교체 */
  public void rotate(String newToken) {
    this.token = requireNonBlank(newToken, "token");
  }

  /** 만료시각 갱신 */
  public void renewTo(Instant newExpiresAt) {
    if (newExpiresAt == null) throw new IllegalArgumentException("expiresAt must not be null");
    this.expiresAt = newExpiresAt;
  }

  /* ===== 유틸 ===== */
  private static String requireNonBlank(String v, String field) {
    if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    return v;
  }
}