package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.Skill; // Skill 엔티티 임포트
import com.shingu.roadmap.common.enums.SkillProficiency;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileSkill {

  @EmbeddedId
  private ProfileSkillId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("profileId") // ProfileSkillId의 profileId 필드에 매핑
  private Profile profile;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("skillId") // ProfileSkillId의 skillId 필드에 매핑
  private Skill skill;

  @Enumerated(EnumType.STRING) // Enum 타입을 DB에 문자열로 저장
  @Column(nullable = false)
  private SkillProficiency proficiency;

  public ProfileSkill(Profile profile, Skill skill, SkillProficiency proficiency) {
    this.id = new ProfileSkillId(profile.getId(), skill.getId());
    this.profile = profile;
    this.skill = skill;
    this.proficiency = proficiency;
  }
}