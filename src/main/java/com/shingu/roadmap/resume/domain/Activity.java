package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "activity",
        indexes = @Index(name = "idx_activity_resume", columnList = "resume_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Activity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title; // 활동명

  @Column(length = 200)
  private String organization; // 소속/기관

  @Embedded
  private Period period; // 활동 기간

  @Column(length = 1000)
  private String description; // 활동 내용

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "resume_id", nullable = false,
          foreignKey = @ForeignKey(name = "fk_activity_resume"))
  private Resume resume;

  /* 양방향 편의 메서드 (Resume.addActivity에서 호출) */
  void setResumeInternal(Resume r) { this.resume = r; }
}