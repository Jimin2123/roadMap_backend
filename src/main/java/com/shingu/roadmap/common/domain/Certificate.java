package com.shingu.roadmap.common.domain;

import com.shingu.roadmap.resume.domain.ResumeCertificate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "certificate",
        indexes = {
                @Index(name = "idx_certificate_qualgbcd", columnList = "qualgbcd"),
                @Index(name = "idx_certificate_jmfldnm", columnList = "jmfldnm")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // for @Builder
@EqualsAndHashCode(of = "jmcd")
@Builder(toBuilder = true)
public class Certificate {

  @Id
  @Column(length = 100, nullable = false)
  private String jmcd; // 종목코드 (PK)

  @Column(length = 100)  // 자격구분명 (예: 국가기술자격 등)
  private String qualgbnm;

  @Column(length = 100)
  private String jmfldnm; // 종목명

  @Column(length = 100)  // 계열명
  private String seriesnm;

  @Column(length = 100)  // 자격구분코드 (예: T/S)
  private String qualgbcd;

  @Column
  private Integer mdobligfldcd; // 대분류 코드

  @Column(length = 50)
  private String mdobligfldnm; // 대분류 명

  @Column
  private Integer obligfldcd; // 중분류 코드

  @Column(length = 50)
  private String obligfldnm; // 중분류 명

  @Column
  private Integer seriescd; // 계열 코드

  @Column(name = "validity_period_years")
  private Integer validityPeriodYears; // 유효기간 (년 단위, NULL=평생 유효)

  @OneToMany(mappedBy = "certificate", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<ResumeCertificate> resumeCertificates = new HashSet<>();

  /* ===== 비즈니스 메서드(선택) ===== */
  // 마스터 데이터로서 변경을 최소화하고 싶다면, 아래 메서드도 제거하거나 접근 제한을 두세요.
  public void changeNames(String qualgbnm, String seriesnm, String jmfldnm) {
    this.qualgbnm = normalize(qualgbnm);
    this.seriesnm = normalize(seriesnm);
    this.jmfldnm  = normalize(jmfldnm);
  }

  public void changeGroupCode(String qualgbcd) {
    this.qualgbcd = normalize(qualgbcd);
  }

  /**
   * 자격증이 특정 날짜 기준으로 유효한지 확인합니다.
   *
   * @param currentDate 기준 날짜 (보통 현재 날짜)
   * @param acquiredYear 취득 연도 (예: "2020")
   * @return 유효 여부
   */
  public boolean isValid(LocalDate currentDate, String acquiredYear) {
    // 1. 유효기간이 설정되지 않은 경우 (평생 유효)
    if (this.validityPeriodYears == null) {
      return true;
    }

    // 2. 취득 연도가 없는 경우 보수적으로 유효하다고 판단
    if (acquiredYear == null || acquiredYear.isBlank()) {
      return true;
    }

    try {
      // 3. 취득 연도를 LocalDate로 변환 (매년 1월 1일로 가정)
      int year = Integer.parseInt(acquiredYear.trim());
      LocalDate acquiredDate = LocalDate.of(year, 1, 1);

      // 4. 만료일 계산
      LocalDate expirationDate = acquiredDate.plusYears(this.validityPeriodYears);

      // 5. 현재 날짜가 만료일 이전이면 유효
      return currentDate.isBefore(expirationDate);

    } catch (NumberFormatException e) {
      // 파싱 실패 시 보수적으로 유효하다고 판단
      return true;
    }
  }

  /**
   * 자격증이 현재 날짜 기준으로 유효한지 확인합니다.
   *
   * @param acquiredYear 취득 연도
   * @return 유효 여부
   */
  public boolean isValidNow(String acquiredYear) {
    return isValid(LocalDate.now(), acquiredYear);
  }

  /* 양방향 편의(필요 시) — ResumeCertificate에서 certificate를 교체할 때 사용 */
  void addResumeCertificate(ResumeCertificate rc) {
    if (rc != null) this.resumeCertificates.add(rc);
  }
  void removeResumeCertificate(ResumeCertificate rc) {
    if (rc != null) this.resumeCertificates.remove(rc);
  }

  private static String normalize(String v) {
    return (v == null || v.isBlank()) ? null : v.trim();
  }
}