package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.auth.domain.Account;
import com.shingu.roadmap.auth.domain.RefreshToken;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "member",
        indexes = {
                @Index(name = "idx_member_name", columnList = "name"),
                @Index(name = "idx_member_role", columnList = "role")
        })
@NamedEntityGraph( // ← 연관 한번에 패치하고 싶을 때 사용
        name = "Member.withAccountProfileAddress",
        attributeNodes = {
                @NamedAttributeNode("account"),
                @NamedAttributeNode("profile"),
                @NamedAttributeNode("address")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
@Builder(toBuilder = true)
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 20)
  private String role;

  private LocalDate birthDate;

  @Column(length = 30)
  private String phoneNumber;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
  @JoinColumn(name = "account_id", unique = true, nullable = false)
  private Account account;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "address_id", unique = true)
  private Address address;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "profile_id", unique = true)
  private Profile profile;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "member_id")
  @Builder.Default
  private List<RecommendedTraining> recommendedTrainings = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "refresh_token_id", unique = true)
  private RefreshToken refreshToken;

  /* ===== 비즈니스 메서드 ===== */

  public void changeName(String newName) { this.name = requireNonBlank(newName, "name"); }
  public void changeRole(String newRole) { this.role = requireNonBlank(newRole, "role"); }
  public void changeBirthDate(LocalDate newBirthDate) { this.birthDate = newBirthDate; }
  public void changePhone(String newPhone) { this.phoneNumber = normalize(newPhone); }

  public void setAccount(Account account) {
    if (account == null) throw new IllegalArgumentException("account must not be null");
    this.account = account; // 주인 쪽만 세팅 — 역방향은 필요 시 서비스에서 보장
  }

  public void setAddress(Address address) { this.address = address; }
  public void clearAddress() { this.address = null; }

  /**
   * Profile을 설정합니다. 양방향 일관성을 유지합니다.
   *
   * @param profile 설정할 Profile
   */
  public void setProfile(Profile profile) {
    // 기존 profile이 있으면 먼저 연결 해제
    if (this.profile != null && !Objects.equals(this.profile, profile)) {
      Profile oldProfile = this.profile;
      this.profile = null;
      // 양방향 일관성 유지를 위해 리플렉션 사용 방지, 단순 참조 변경만 수행
    }

    this.profile = profile;
    // 양방향 일관성은 JPA가 관리하지만, 도메인 로직에서도 명시적으로 확인 가능
  }

  /**
   * Profile을 제거합니다. orphanRemoval=true이므로 Profile도 함께 삭제됩니다.
   */
  public void clearProfile() {
    this.profile = null;
  }

  public void updateRefreshToken(RefreshToken token) { this.refreshToken = token; }

  public void addRecommendedTraining(RecommendedTraining rt) {
    if (rt != null && !this.recommendedTrainings.contains(rt)) this.recommendedTrainings.add(rt);
  }

  public void removeRecommendedTraining(RecommendedTraining rt) {
    if (rt != null) this.recommendedTrainings.remove(rt);
  }

  /* ===== 편의 메서드 (선택) ===== */
  public String getEmail() {
    return (account != null && account.getEmail() != null) ? account.getEmail().getValue() : null;
  }

  /* ===== 유틸 ===== */
  private static String requireNonBlank(String v, String field) {
    if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    return v;
  }
  private static String normalize(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}