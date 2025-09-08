package com.shingu.roadmap.resume.domain;

import com.shingu.roadmap.common.domain.Skill;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  private String url; // 프로젝트 URL (예: GitHub 링크)
  private String role; // 프로젝트에서의 역할 (예: 프론트엔드 개발자)

  @Column(length = 2000)
  private String description; // 프로젝트 설명 (최대 2000자)

  @Embedded
  private Period period; // 프로젝트 기간 (예: 2023.01 - 2023.06)

  @ElementCollection // 간단한 문자열 리스트를 저장하기 위한 어노테이션
  private List<String> achievements = new ArrayList<>(); // 성과 추가

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resume_id")
  private Resume resume;

  @ManyToMany
  @JoinTable(
          name = "project_skill",
          joinColumns = @JoinColumn(name = "project_id"),
          inverseJoinColumns = @JoinColumn(name = "skill_id")
  )
  private Set<Skill> techStack = new HashSet<>();
}
