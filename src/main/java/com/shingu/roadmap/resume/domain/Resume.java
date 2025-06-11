package com.shingu.roadmap.resume.domain;

import com.shingu.roadmap.resume.dto.request.ActivityRequest;
import com.shingu.roadmap.resume.dto.request.EducationRequest;
import com.shingu.roadmap.resume.dto.request.PortfolioRequest;
import com.shingu.roadmap.resume.dto.request.ProjectRequest;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Resume {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "introduction_id")
  private Introduction introduction;

  @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Activity> activities;

  @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Portfolio> portfolios;

  @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Project> projects;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "education_id")
  private Education education;
}