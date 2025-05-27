package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Profile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String educationLevel; // 학력 수준 (예: 고등학교, 대학교 등)

  @Column(length = 100)
  private String major; // 전공 (예: 컴퓨터공학, 경영학 등)

  @Column(length = 100)
  private String desiredJob; // 희망 직무 (예: 소프트웨어 개발자, 데이터 분석가 등)

  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "certificate_id")
  private Certificate certificate; // 자격증 정보

  @ManyToMany
  @JoinTable(
          name = "profile_skill",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "skill_id")
  )
  private Set<Skill> skills = new HashSet<>(); // 보유 기술

  @ManyToMany
  @JoinTable(
          name = "profile_desired_ncs",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "ncs_code")
  )
  private Set<NcsOccupation> desiredCapabilities = new HashSet<>(); // 희망 직업 NCS 코드

  @ManyToMany
  @JoinTable(
          name = "profile_user_ncs",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "ncs_code")
  )
  private Set<NcsOccupation> userCapabilities = new HashSet<>(); // 보유 NCS 코드

  public void addSkill(Skill skill) {
    skills.add(skill);
  }
}
