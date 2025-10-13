package com.shingu.roadmap.member.domain;

import com.shingu.roadmap.apis.ncs.domain.NcsOccupation;
import com.shingu.roadmap.apis.saramin.domain.SaraminJob;
import com.shingu.roadmap.resume.domain.Resume;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
@Builder(toBuilder = true)
@NamedEntityGraph( // DTO 변환 시 필요한 연관을 한 번에 로딩할 때 사용
        name = "Profile.graph.forResponse",
        attributeNodes = {
                @NamedAttributeNode("desiredJobs"),
                @NamedAttributeNode("profileSkills"),
                @NamedAttributeNode("desiredCapabilities"),
                @NamedAttributeNode("userCapabilities"),
                @NamedAttributeNode("resume")
        }
)
public class Profile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String educationLevel;

  @Column(length = 500)
  private String profileImageUrl;

  @Column(length = 64)
  private String recommendedJobInfoCategoryCode;

  @Column(length = 128)
  private String recommendedJobInfoAbilityCode;

  @Column(length = 64)
  private String recommendedEncyclopediaThemeCode;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "profile_desired_job",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "job_code"))
  @Builder.Default
  private Set<SaraminJob> desiredJobs = new HashSet<>();

  @OneToMany(mappedBy = "profile", fetch = FetchType.LAZY,
          cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ProfileSkill> profileSkills = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "profile_desired_ncs",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "ncs_code"))
  @Builder.Default
  private Set<NcsOccupation> desiredCapabilities = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "profile_user_ncs",
          joinColumns = @JoinColumn(name = "profile_id"),
          inverseJoinColumns = @JoinColumn(name = "ncs_code"))
  @Builder.Default
  private Set<NcsOccupation> userCapabilities = new HashSet<>();

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "resume_id")
  private Resume resume;

  @OneToOne(mappedBy = "profile", fetch = FetchType.LAZY)
  private Member member;

  /* ===== 팩토리 메서드 ===== */

  /**
   * Profile 생성 팩토리 메서드.
   * Profile은 Member와의 관계를 통해서만 존재해야 하므로, 고아 Profile 생성을 방지합니다.
   *
   * @return 새로운 Profile 인스턴스
   */
  public static Profile createProfile() {
    return Profile.builder()
            .desiredJobs(new HashSet<>())
            .profileSkills(new HashSet<>())
            .desiredCapabilities(new HashSet<>())
            .userCapabilities(new HashSet<>())
            .build();
  }

  /* ===== 비즈니스 메서드 ===== */

  /**
   * Profile이 Member에 속해있는지 확인합니다.
   *
   * @return Member에 속해있으면 true, 아니면 false
   */
  public boolean isAttachedToMember() {
    return this.member != null;
  }

  public void updateEducationLevel(String v) { this.educationLevel = normalize(v); }
  public void updateProfileImageUrl(String v) { this.profileImageUrl = normalize(v); }
  public void updateRecommendedJobInfoCategoryCode(String v) { this.recommendedJobInfoCategoryCode = normalize(v); }
  public void updateRecommendedJobInfoAbilityCode(String v) { this.recommendedJobInfoAbilityCode = normalize(v); }
  public void updateRecommendedEncyclopediaThemeCode(String v) { this.recommendedEncyclopediaThemeCode = normalize(v); }

  public void setResume(Resume r) { this.resume = r; }
  public void clearResume() { this.resume = null; }

  public void addDesiredJob(SaraminJob job) { if (job != null) this.desiredJobs.add(job); }
  public void removeDesiredJob(SaraminJob job) { if (job != null) this.desiredJobs.remove(job); }

  public void addDesiredCapability(NcsOccupation ncs) { if (ncs != null) this.desiredCapabilities.add(ncs); }
  public void removeDesiredCapability(NcsOccupation ncs) { if (ncs != null) this.desiredCapabilities.remove(ncs); }

  public void addUserCapability(NcsOccupation ncs) { if (ncs != null) this.userCapabilities.add(ncs); }
  public void removeUserCapability(NcsOccupation ncs) { if (ncs != null) this.userCapabilities.remove(ncs); }

  /* --- 양방향 일관성: 자식이 주인(mappedBy="profile")이므로 setProfile(this) 필요 --- */
  public void addSkill(ProfileSkill ps) {
    if (ps == null) return;
    if (this.profileSkills.add(ps)) {
      if (!Objects.equals(ps.getProfile(), this)) ps.setProfile(this);
    }
  }

  public void removeSkill(ProfileSkill ps) {
    if (ps == null) return;
    if (this.profileSkills.remove(ps)) {
      if (Objects.equals(ps.getProfile(), this)) ps.setProfile(null);
    }
  }

  private static String normalize(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}