package com.shingu.roadmap.member.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "address",
        indexes = {
                @Index(name = "idx_address_region_city", columnList = "regionCity"),
                @Index(name = "idx_address_zonecode", columnList = "zonecode")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class Address {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 실무 안전 폭: 255
  @Column(length = 255)
  private String address;       // 도로명 주소

  @Column(length = 255)
  private String addressJibun;  // 지번 주소

  @Column(length = 255)
  private String addressDetail; // 상세 주소

  @Column(length = 100)
  private String regionCity;    // 시/군/구

  // 외부 포맷/확장성 고려하여 길이 여유
  @Column(length = 20)
  private String zonecode;      // 우편번호

  @OneToOne(mappedBy = "address", fetch = FetchType.LAZY)
  private Member member;

  @Builder
  private Address(String address,
                  String addressJibun,
                  String addressDetail,
                  String regionCity,
                  String zonecode) {
    this.address = normalize(address);
    this.addressJibun = normalize(addressJibun);
    this.addressDetail = normalize(addressDetail);
    this.regionCity = normalize(regionCity);
    this.zonecode = normalize(zonecode);
  }

  /* Member와의 양방향 편의 (Member.setAddress에서 사용) */
  void linkMember(Member m) { this.member = m; }
  void unlinkMember() { this.member = null; }

  private static String normalize(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }

  /* (선택) 의미 있는 변경 메서드 제공 — @Setter 대신 의도가 드러나게 */
  public void relocate(String newAddress, String newJibun, String newDetail, String newRegionCity, String newZonecode) {
    this.address = normalize(newAddress);
    this.addressJibun = normalize(newJibun);
    this.addressDetail = normalize(newDetail);
    this.regionCity = normalize(newRegionCity);
    this.zonecode = normalize(newZonecode);
  }
}