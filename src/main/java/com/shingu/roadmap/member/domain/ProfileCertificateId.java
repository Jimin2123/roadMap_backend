package com.shingu.roadmap.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class ProfileCertificateId implements Serializable {
  @Column(name = "profile_id", nullable = false)
  private Long profileId;

  @Column(name = "certificate_jmcd", nullable = false) // ← 컬럼명 FK와 일치
  private String certificateId; // Certificate.jmcd = String
}