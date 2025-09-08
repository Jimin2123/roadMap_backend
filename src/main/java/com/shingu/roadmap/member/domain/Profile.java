package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.common.domain.Certificate;
import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.enums.SkillProficiency;
import com.shingu.roadmap.resume.domain.Resume;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Profile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String educationLevel; // 학력 수준 (예: 고등학교, 대학교 등)

  // 추천된 검색 코드를 저장할 필드
  private String recommendedJobInfoCategoryCode; // 직업정보 - 직업 분야
  private String recommendedJobInfoAbilityCode;  // 직업정보 - 핵심 역량 (여러 개일 수 있으므로 JSON이나 별도 테이블 고려)
  private String recommendedEncyclopediaThemeCode; // 직업백과 - 테마

  @ManyToMany
  @JoinTable(name = "profile_desired_job",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "job_code"))
  private Set<SaraminJob> desiredJobs = new HashSet<>();

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProfileCertificate> profileCertificates = new HashSet<>();

  @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ProfileSkill> profileSkills = new HashSet<>();

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

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "resume_id")
  private Resume resume;
}
