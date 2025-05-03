package com.shingu.roadmap.apis.ncs.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NcsOccupationStandardLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "occupation_code")
  private NcsOccupation occupation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "training_standard_id")
  private NcsTrainingStandard trainingStandard;

  public NcsOccupationStandardLink(NcsOccupation occupation, NcsTrainingStandard standard) {
    this.occupation = occupation;
    this.trainingStandard = standard;
  }
}
