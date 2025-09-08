package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.Certificate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(name = "profile_certificate")
public class ProfileCertificate {

  @EmbeddedId
  private ProfileCertificateId id;

  @MapsId("profileId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "profile_id",
          foreignKey = @ForeignKey(name = "fk_profilecert_profile"))
  private Profile profile;

  @MapsId("certificateId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "certificate_jmcd",
          foreignKey = @ForeignKey(name = "fk_profilecert_certificate"))
  private Certificate certificate;

  /** 연도(YYYY) 또는 확장 포맷을 고려해 길이 8 유지 */
  @Column(length = 8)
  private String acquiredYear;

  @Builder
  private ProfileCertificate(Profile profile, Certificate certificate, String acquiredYear) {
    if (profile == null || certificate == null) {
      throw new IllegalArgumentException("profile/certificate must not be null");
    }
    // ⚠️ 여기서 id를 즉시 만들지 않고, 연관만 세팅 (PK는 @PrePersist에서 생성)
    this.profile = profile;
    this.certificate = certificate;
    this.acquiredYear = normalize(acquiredYear);
  }

  /** 영속화 직전, 연관의 PK를 사용해 복합키를 안전하게 구성 */
  @PrePersist
  private void assignIdIfNeeded() {
    if (this.id == null) {
      Long profileId = (profile != null) ? profile.getId() : null;
      String certificateKey = (certificate != null) ? certificate.getJmcd() : null;
      if (profileId == null || certificateKey == null) {
        throw new IllegalStateException("ProfileCertificate cannot be persisted with null profileId/certificateId");
      }
      this.id = new ProfileCertificateId(profileId, certificateKey);
    }
  }

  /* 변경 메서드 */
  public void changeAcquiredYear(String year) { this.acquiredYear = normalize(year); }

  /* 양방향 편의(필요 시 Profile.addCertificate(...)에서 사용) — 외부 공개 안 함 */
  void setProfile(Profile p) { this.profile = p; }
  void setCertificate(Certificate c) { this.certificate = c; }

  public static ProfileCertificate of(Profile profile, Certificate certificate, String acquiredYear) {
    return ProfileCertificate.builder()
            .profile(profile)
            .certificate(certificate)
            .acquiredYear(acquiredYear)
            .build();
  }

  private static String normalize(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}