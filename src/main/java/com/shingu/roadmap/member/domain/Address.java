package com.shingu.roadmap.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Address {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String address; // 도로명 주소

  @Column(length = 100)
  private String addressJibun; // 지번 주소

  @Column(length = 100)
  private String addressDetail; // 상세 주소

  @Column(length = 100)
  private String zoncode; // 우편번호

  @OneToOne(mappedBy = "address")
  private Member member;
}
