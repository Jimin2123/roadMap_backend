package com.shingu.roadmap.common.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skill",
        indexes = @Index(name = "idx_skill_name", columnList = "name", unique = true))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder 전용
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Skill {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  /* ===== 비즈니스 메서드 ===== */
  public void rename(String newName) {
    this.name = normalize(newName);
  }

  private static String normalize(String v) {
    return (v == null || v.isBlank()) ? null : v.trim();
  }
}