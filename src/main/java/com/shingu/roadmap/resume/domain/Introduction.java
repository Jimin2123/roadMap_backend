package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Introduction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "growth_process")
  private String growthProcess; // 성장과정

  @Column(name = "strengths")
  private String strengths; // 장점 및 강점

  @Column(name = "school_life")
  private String schoolLife; // 학교생활

  @Column(name = "motivation")
  private String motivation; // 지원동기
}