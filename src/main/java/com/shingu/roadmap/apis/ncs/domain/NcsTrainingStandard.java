package com.shingu.roadmap.apis.ncs.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NcsTrainingStandard {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String itemName; // 항목 구분 명

  @Column(unique = true)
  private String defText; // 항목 세부 내용

  @OneToMany(mappedBy = "trainingStandard")
  private List<NcsOccupationStandardLink> occupations = new ArrayList<>();

  public NcsTrainingStandard(String itemName, String defText) {
    this.itemName = itemName;
    this.defText = defText;
  }
}
