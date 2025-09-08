package com.shingu.roadmap.common.domain;

import com.shingu.roadmap.member.domain.ProfileCertificate;
import jakarta.persistence.*;
import lombok.*;

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

  @OneToMany(mappedBy = "certificate", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<ProfileCertificate> profileCertificates = new HashSet<>();

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

  /* 양방향 편의(필요 시) — ProfileCertificate에서 certificate를 교체할 때 사용 */
  void addProfileCertificate(ProfileCertificate pc) {
    if (pc != null) this.profileCertificates.add(pc);
  }
  void removeProfileCertificate(ProfileCertificate pc) {
    if (pc != null) this.profileCertificates.remove(pc);
  }

  private static String normalize(String v) {
    return (v == null || v.isBlank()) ? null : v.trim();
  }
}