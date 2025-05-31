package com.shingu.roadmap.apis.saramin.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SaraminJobKeyword {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name; // ex) 리스크 분석, 리스크 관리

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_code", nullable = false)
  private SaraminJob job;
}