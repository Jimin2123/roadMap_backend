package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.enums.SkillProficiency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
@Table(name = "profile_skill")
@Builder(toBuilder = true)
public class ProfileSkill {

  @EmbeddedId
  @Builder.Default
  private ProfileSkillId id = new ProfileSkillId(); // ★ null 금지

  @MapsId("profileId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "profile_id",
          foreignKey = @ForeignKey(name = "fk_profileskill_profile"))
  private Profile profile;

  @MapsId("skillId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "skill_id",
          foreignKey = @ForeignKey(name = "fk_profileskill_skill"))
  private Skill skill;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private SkillProficiency proficiency;


  @Builder
  private ProfileSkill(Profile profile, Skill skill, SkillProficiency proficiency) {
    if (profile == null || skill == null || proficiency == null)
      throw new IllegalArgumentException("profile/skill/proficiency must not be null");

    this.profile = profile;   // @MapsId("profileId")가 채움
    this.skill = skill;       // @MapsId("skillId")가 채움
    this.proficiency = proficiency;
  }

  public void changeProficiency(SkillProficiency p) {
    if (p == null) throw new IllegalArgumentException("proficiency must not be null");
    this.proficiency = p;
  }

  // 편의 세터 (패키지-프라이빗 유지)
  void setProfile(Profile profile) { this.profile = profile; }
  void setSkill(Skill skill) { this.skill = skill; }

  public static ProfileSkill of(Profile profile, Skill skill, SkillProficiency proficiency) {
    return ProfileSkill.builder()
            .profile(profile)
            .skill(skill)
            .proficiency(proficiency)
            .build();
  }
}