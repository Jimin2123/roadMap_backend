package com.shingu.roadmap.apis.ncs.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
  private String dutyDef; // 직무 설명

  @OneToMany(mappedBy = "occupation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<NcsOccupationStandardLink> trainingLinks = new ArrayList<>();

  public NcsOccupation(String dutyCd, String dutyNm, String dutySvcNo, String dutyDef) {
    this.dutyCd = dutyCd;
    this.dutyNm = dutyNm;
    this.dutySvcNo = dutySvcNo;
    this.dutyDef = dutyDef;
    this.trainingLinks = new ArrayList<>();
  }

  public boolean hasTrainingStandard(NcsTrainingStandard standard) {
    return this.trainingLinks.stream()
            .anyMatch(link -> link.getTrainingStandard().equals(standard));
  }
}
