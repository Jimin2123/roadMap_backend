package com.shingu.roadmap.resume.domain;

import com.shingu.roadmap.common.domain.Skill;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "project",
        indexes = @Index(name = "idx_project_resume", columnList = "resume_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name; // 프로젝트 이름

  @Column(length = 300)
  private String url; // 프로젝트 URL (예: GitHub 링크)

  @Column(length = 100)
  private String role; // 역할 (예: 프론트엔드 개발자)

  @Column(length = 2000)
  private String description; // 프로젝트 설명

  @Embedded
  private Period period; // 프로젝트 기간

  @ElementCollection
  @CollectionTable(name = "project_achievement", joinColumns = @JoinColumn(name = "project_id"))
  @Column(name = "achievement", length = 500)
  @Builder.Default
  private List<String> achievements = new ArrayList<>(); // 성과

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "resume_id", nullable = false,
          foreignKey = @ForeignKey(name = "fk_project_resume"))
  private Resume resume;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
          name = "project_skill",
          joinColumns = @JoinColumn(name = "project_id"),
          inverseJoinColumns = @JoinColumn(name = "skill_id")
  )
  @Builder.Default
  private Set<Skill> techStack = new HashSet<>();

  /* 양방향 편의 메서드 (Resume.addProject에서 호출) */
  void setResumeInternal(Resume r) { this.resume = r; }

  /* 비즈니스 메서드 예시 */
  public void addAchievement(String a) {
    if (a != null && !a.isBlank()) achievements.add(a.trim());
  }
}