package com.shingu.roadmap.resume.domain;

import com.shingu.roadmap.common.enums.SalaryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "desired_company",
  indexes = @Index(name = "idx_desired_company_resume", columnList = "resume_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode(of = "id")
public class DesiredCompany {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String desiredCompany1; // 희망 회사 1

  @Column(length = 100)
  private String desiredCompany2; // 희망 회사 2

  @Column(length = 100)
  private String desiredRegion; // 희망 지역

  @Enumerated(EnumType.STRING)
  private SalaryType salaryType; // 희망 급여 형태

  @Column
  private int desiredSalary; // 희망 급여 금액

  @Column(length = 1000)
  private String careerPlan; // 진로 계획

}
