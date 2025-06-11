package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.common.domain.Certificate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileCertificate {

  @EmbeddedId
  private ProfileCertificateId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("profileId")
  private Profile profile;

  @MapsId("certificateId") // 복합 키 안의 certificateId 필드와 연결
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "certificate_jmcd")
  private Certificate certificate;

  private String acquiredYear;

  public ProfileCertificate(Profile profile, Certificate certificate, String year) {
    this.id = new ProfileCertificateId(profile.getId(), certificate.getJmcd());
    this.profile = profile;
    this.certificate = certificate;
    this.acquiredYear = year;
  }
}

