package com.shingu.roadmap.common.domain;

import com.shingu.roadmap.member.domain.ProfileCertificate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Certificate {
  @Id
  @Column(length = 100)
  private String jmcd; // 종목코드 (PK)

  @Column(length = 100)
  private String qualgbnm; // 자격구분명

  @Column(length = 100)
  private String seriesnm; // 계열명

  @Column(length = 100)
  private String qualgbcd; // 자격구분코드 (T/S)

  @Column(length = 100)
  private String jmfldnm; // 종목명

  @OneToMany(mappedBy = "certificate")
  private Set<ProfileCertificate> profileCertificates = new HashSet<>();
}
