package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.common.enums.SkillProficiency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(name = "profile_skill")
public class ProfileSkill {

  @EmbeddedId
  private ProfileSkillId id;

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
    // ⚠ 여기서 id를 즉시 만들지 않고 연관만 세팅
    this.profile = profile;
    this.skill = skill;
    this.proficiency = proficiency;
  }

  /** 영속화 직전, 연관의 PK를 사용해 복합키를 안전하게 구성 */
  @PrePersist
  private void assignIdIfNeeded() {
    if (this.id == null) {
      Long profileId = (profile != null) ? profile.getId() : null;
      Long skillId = (skill != null) ? skill.getId() : null;
      if (profileId == null || skillId == null) {
        throw new IllegalStateException("ProfileSkill cannot be persisted with null profileId/skillId");
      }
      this.id = new ProfileSkillId(profileId, skillId);
    }
  }

  public void changeProficiency(SkillProficiency p) {
    if (p == null) throw new IllegalArgumentException("proficiency must not be null");
    this.proficiency = p;
  }

  /* ===== 양방향 일관성을 위한 (패키지-프라이빗) 세터 — 필요 시 Profile 편의 메서드에서 호출 ===== */
  void setProfile(Profile profile) { this.profile = profile; }
  void setSkill(Skill skill) { this.skill = skill; }

  /* ===== 정적 팩토리 (가독성/의도 표현용) ===== */
  public static ProfileSkill of(Profile profile, Skill skill, SkillProficiency proficiency) {
    return ProfileSkill.builder()
            .profile(profile)
            .skill(skill)
            .proficiency(proficiency)
            .build();
  }
}