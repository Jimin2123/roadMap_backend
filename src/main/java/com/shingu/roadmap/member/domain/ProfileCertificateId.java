package com.shingu.roadmap.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProfileCertificateId implements Serializable {

  @Column(name = "profile_id", nullable = false)
  private Long profileId;

  /** PK/FK 실제 컬럼명을 엔티티의 @JoinColumn(name="certificate_jmcd")와 일치시킵니다. */
  @Column(name = "certificate_jmcd", nullable = false)
  private String certificateId; // Certificate.jmcd
}