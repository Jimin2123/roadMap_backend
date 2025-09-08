package com.shingu.roadmap.auth.domain;

import com.shingu.roadmap.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "account",
        indexes = {
                @Index(name = "idx_account_email", columnList = "email", unique = true),
                @Index(name = "idx_account_last_login", columnList = "lastLogin")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // for @Builder
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(nullable = false, length = 100)
  private String password; // 해시 보관 가정

  @Column
  private LocalDateTime lastLogin;

  // Member가 주인(JoinColumn은 Member에 있음) → 여기서는 역방향만 보유
  @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
  private Member member;

  /* ===== 비즈니스 메서드 ===== */

  /** 로그인 성공 시점 기록 */
  public void markLoggedIn(LocalDateTime when) {
    this.lastLogin = (when != null) ? when : LocalDateTime.now();
  }

  /** 이메일 변경 (중복/형식 검증은 도메인外 정책에서 수행) */
  public void changeEmail(String newEmail) {
    this.email = requireNonBlank(newEmail, "email");
  }

  /** 비밀번호(해시) 변경 */
  public void changePassword(String newHashedPassword) {
    this.password = requireNonBlank(newHashedPassword, "password");
  }

  /* ===== 연관 편의 (Member.setAccount에서 호출) ===== */
  void linkMember(Member m) { this.member = m; }
  void unlinkMember() { this.member = null; }

  /* ===== 유틸 ===== */
  private static String requireNonBlank(String v, String field) {
    if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    return v;
  }
}