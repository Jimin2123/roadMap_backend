package com.shingu.roadmap.apis.work24.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Work24EmploymentCenter {
  @Id
  private String categoryId;
  private String regionOffice;
  private String centerName;
}
