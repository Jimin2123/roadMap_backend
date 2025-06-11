package com.shingu.roadmap.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCertificateId implements Serializable {

  private Long profileId;

  @Column(name = "certificate_jmcd") // 반드시 동일하게 지정
  private String certificateId; // Certificate의 PK는 jmcd(String)

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ProfileCertificateId)) return false;
    ProfileCertificateId that = (ProfileCertificateId) o;
    return Objects.equals(profileId, that.profileId) &&
            Objects.equals(certificateId, that.certificateId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profileId, certificateId);
  }
}