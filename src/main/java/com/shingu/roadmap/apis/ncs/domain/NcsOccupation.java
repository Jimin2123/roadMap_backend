package com.shingu.roadmap.apis.ncs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class NcsOccupation {
  @Id
  @Column(name = "ncs_code", nullable = false)
  private String dutyCd;
  private String dutyNm; // 직무명
  private String dutySvcNo; // 직무 서비스 번호

  @Column(columnDefinition = "TEXT")
  private String dutySvcNm; // 직무 서비스 명
}
