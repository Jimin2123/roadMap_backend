package com.shingu.roadmap.resume.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@NamedEntityGraph( // ← 한 번에 로딩하고 싶을 때 사용
        name = "Resume.withAll",
        attributeNodes = {
                @NamedAttributeNode("introduction"),
                @NamedAttributeNode("education"),
                @NamedAttributeNode("activities"),
                @NamedAttributeNode("projects"),
                @NamedAttributeNode("careers"),
                @NamedAttributeNode("certificates")
        }
)
public class Resume {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "introduction_id",
          foreignKey = @ForeignKey(name = "fk_resume_intro"))
  private Introduction introduction;

  @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY,
          cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC") // 필요에 맞게 "id DESC"나 "period.startDate DESC" 등으로 변경
  @Builder.Default
  private List<Activity> activities = new ArrayList<>();

  @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY,
          cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  @Builder.Default
  private List<Project> projects = new ArrayList<>();

  @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY,
          cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  @Builder.Default
  private List<Career> careers = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "education_id",
          foreignKey = @ForeignKey(name = "fk_resume_education"))
  private Education education;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "desired_company_id",
          foreignKey = @ForeignKey(name = "fk_resume_desired_company"))
  private DesiredCompany desiredCompany;

  @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY,
          cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ResumeCertificate> certificates = new HashSet<>();

  /* ===== 편의/비즈니스 메서드 ===== */

  public void setIntroduction(Introduction intro) {
    this.introduction = intro; // 단방향
  }

  public void clearIntroduction() { this.introduction = null; }

  public void setEducation(Education edu) {
    this.education = edu; // 단방향
  }

  public void clearEducation() { this.education = null; }

  public void setDesiredCompany(DesiredCompany desiredCompany) {
    this.desiredCompany = desiredCompany;
  }

  public void clearDesiredCompany() {
    this.desiredCompany = null;
  }

  public void addActivity(Activity a) {
    if (a == null) return;
    if (!Objects.equals(a.getResume(), this)) {
      a.setResumeInternal(this);
    }
    this.activities.add(a);
  }

  public void removeActivity(Activity a) {
    if (a == null) return;
    if (this.activities.remove(a)) {
      a.setResumeInternal(null);
    }
  }

  public void addProject(Project p) {
    if (p == null) return;
    if (!Objects.equals(p.getResume(), this)) {
      p.setResumeInternal(this);
    }
    this.projects.add(p);
  }

  public void removeProject(Project p) {
    if (p == null) return;
    if (this.projects.remove(p)) {
      p.setResumeInternal(null);
    }
  }

  public void addCareer(Career c) {
    if (c == null) return;
    if (!Objects.equals(c.getResume(), this)) {
      c.setResumeInternal(this);
    }
    this.careers.add(c);
  }

  public void removeCareer(Career c) {
    if (c == null) return;
    if (this.careers.remove(c)) {
      c.setResumeInternal(null);
    }
  }

  public void addCertificate(ResumeCertificate rc) {
    if (rc == null) return;
    if (this.certificates.add(rc)) {
      if (!Objects.equals(rc.getResume(), this)) {
        rc.setResume(this);
      }
    }
  }

  public void removeCertificate(ResumeCertificate rc) {
    if (rc == null) return;
    if (this.certificates.remove(rc)) {
      if (Objects.equals(rc.getResume(), this)) {
        rc.setResume(null);
      }
    }
  }
}