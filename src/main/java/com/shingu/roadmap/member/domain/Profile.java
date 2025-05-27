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
  private String educationLevel;

  @Column(length = 100)
  private String major;

  @Column(length = 100)
  private String desiredJob;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "certificate_id")
  private Certificate certificate;

  @ManyToMany
  @JoinTable(
          name = "profile_skill",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "skill_id")
  )
  private Set<Skill> skills = new HashSet<>();

  @ManyToMany
  @JoinTable(
          name = "profile_desired_ncs",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "ncs_code")
  )
  private Set<NcsOccupation> desiredCapabilities = new HashSet<>();

  @ManyToMany
  @JoinTable(
          name = "profile_user_ncs",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "ncs_code")
  )
  private Set<NcsOccupation> userCapabilities = new HashSet<>();

  public void addSkill(Skill skill) {
    skills.add(skill);
  }
}
