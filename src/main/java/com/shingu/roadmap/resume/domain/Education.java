package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Education {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String school; // 학교명
  private String major; // 전공
  private String period; // 재학 기간
  private String status; // 학력 상태 (예: 졸업, 재학 중, 중퇴 등)
}