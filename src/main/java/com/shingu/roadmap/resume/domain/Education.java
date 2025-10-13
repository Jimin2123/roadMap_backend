package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Education {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String school; // 학교명

  @Column(length = 200)
  private String major; // 전공

  @Column
  private Double gpa; // 학점 (예: 3.5)

  @Embedded
  private Period period; // 재학 기간

  @Column(length = 50)
  private String status; // 학력 상태 (졸업, 재학 등)

  /* 필요 시 비즈니스 메서드 */
  public void updateStatus(String newStatus) {
    this.status = (newStatus == null || newStatus.isBlank()) ? null : newStatus.trim();
  }
}