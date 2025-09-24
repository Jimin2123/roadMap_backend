package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.Certificate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
@Table(name = "profile_certificate")
@Builder(toBuilder = true)
public class ProfileCertificate {

  @EmbeddedId
  @Builder.Default
  private ProfileCertificateId id = new ProfileCertificateId(); // ★ null 금지

  @MapsId("profileId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "profile_id",
          foreignKey = @ForeignKey(name = "fk_profilecert_profile"))
  private Profile profile;

  @MapsId("certificateId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "certificate_jmcd", // FK 컬럼명과 Embeddable의 certificateId 매핑
          foreignKey = @ForeignKey(name = "fk_profilecert_certificate"))
  private Certificate certificate;

  /** 연도(YYYY) 또는 확장 포맷 고려 */
  @Column(length = 8)
  private String acquiredYear;

  @Builder
  private ProfileCertificate(Profile profile, Certificate certificate, String acquiredYear) {
    if (profile == null || certificate == null) {
      throw new IllegalArgumentException("profile/certificate must not be null");
    }
    this.profile = profile;
    this.certificate = certificate;
    this.acquiredYear = normalize(acquiredYear);
  }

  public static ProfileCertificate of(Profile profile, Certificate certificate, String acquiredYear) {
    return ProfileCertificate.builder()
            .profile(profile)
            .certificate(certificate)
            .acquiredYear(acquiredYear)
            .build();
  }

  public void changeAcquiredYear(String year) { this.acquiredYear = normalize(year); }

  // 편의 세터는 패키지-프라이빗 유지
  void setProfile(Profile p) { this.profile = p; }
  void setCertificate(Certificate c) { this.certificate = c; }

  private static String normalize(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}