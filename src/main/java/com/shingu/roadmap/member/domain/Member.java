package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.auth.domain.Account;
import com.shingu.roadmap.auth.domain.RefreshToken;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Member {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 20)
  private String role; // ADMIN, USER 등

  private LocalDate birthDate; // 생년월일

  private String phoneNumber; // 전화번호

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
  @JoinColumn(name = "account_id", unique = true, nullable = false)
  private Account account;

  @Setter
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "address_id", unique = true)
  private Address address;

  @Setter
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "profile_id", unique = true)
  private Profile profile;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "member_id") // 이게 핵심: 외래 키를 Member 쪽이 관리
  private List<RecommendedTraining> recommendedTrainings = new ArrayList<>();

  @Setter
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "refresh_token_id", unique = true)
  private RefreshToken refreshToken;

  public void setAccount(Account account) {
    this.account = account;
    account.setMember(this);
  }
}