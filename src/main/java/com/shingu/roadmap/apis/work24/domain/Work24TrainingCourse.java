package com.shingu.roadmap.apis.work24.domain;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Work24TrainingCourse {
  @Id
  @Column(length = 100)
  private String trprId; // 훈련과정ID (PK)

  @Column(length = 100)
  private String address;

  @Column(length = 100)
  private String certificate;

  @Column(length = 100)
  private String contents;

  private Integer courseMan;

  @Column(length = 100)
  private String grade;

  @Column(length = 100)
  private String instCd;

  private Integer realMan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ncs_code")
  private NcsOccupation ncsOccupation;

  @Column(length = 100)
  private String regCourseMan;

  @Column(length = 100)
  private String stdgScor;

  @Column(length = 100)
  private String subTitle;

  @Column(length = 100)
  private String subTitleLink;

  @Column(length = 100)
  private String telNo;

  @Column(length = 100)
  private String title;

  @Column(length = 100)
  private String titleIcon;

  @Column(length = 100)
  private String titleLink;

  private LocalDate traStartDate;
  private LocalDate traEndDate;

  @Column(length = 100)
  private String trainTarget;

  @Column(length = 100)
  private String trainstCstId;

  private Integer trngAreaCd;

  @Column(length = 100)
  private String trprDegr;

  private Integer yardMan;
}
