package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title; // 활동명
  private String organization; // 소속/기관
  private String period; // 활동 기간

  @Column(length = 1000)
  private String description; // 활동 내용

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resume_id")
  private Resume resume;
}
