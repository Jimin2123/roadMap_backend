package com.shingu.roadmap.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProfileSkillId implements Serializable {

  @Column(name = "profile_id", nullable = false)
  private Long profileId;

  @Column(name = "skill_id", nullable = false)
  private Long skillId; // Skill의 PK(Long) 가정
}
