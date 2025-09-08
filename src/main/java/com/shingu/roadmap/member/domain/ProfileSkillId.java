package com.shingu.roadmap.member.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSkillId implements Serializable {

  private Long profileId;
  private Long skillId; // Skill 엔티티의 ID 타입이 Long이라고 가정

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProfileSkillId that = (ProfileSkillId) o;
    return Objects.equals(profileId, that.profileId) && Objects.equals(skillId, that.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profileId, skillId);
  }
}