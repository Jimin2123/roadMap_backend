package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.enums.EducationLevelType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Profile {

  @Enumerated(value = EnumType.STRING)
  private EducationLevelType educationLevel; // 학력

  private String major; // 전공
  private String desiredJob; // 희망 직무
}
