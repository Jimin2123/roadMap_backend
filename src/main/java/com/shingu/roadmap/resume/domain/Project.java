package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name; // 프로젝트 이름
  private String period; // 프로젝트 기간 (예: 2023.01 - 2023.06)
  private String techStack; // 사용 기술 스택 (예: Java, Spring Boot, React 등)

  @Column(length = 2000)
  private String description; // 프로젝트 설명 (최대 2000자)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resume_id")
  private Resume resume;
}
