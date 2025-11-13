package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.common.dto.CertificateDTO;
import com.shingu.roadmap.resume.domain.Activity;
import com.shingu.roadmap.resume.domain.Career;
import com.shingu.roadmap.resume.domain.Project;
import com.shingu.roadmap.resume.domain.Resume;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ResumeResponse(
        IntroductionResponse introduction,
        EducationResponse education,
        DesiredCompanyResponse desiredCompany,
        List<CareerResponse> careers,
        List<ActivityResponse> activities,
        List<ProjectResponse> projects,
        Set<CertificateDTO> certificates
) {
  public static ResumeResponse from(Resume resume) {
    if (resume == null) return null;

    Comparator<Activity> activityOrder = Comparator
            .comparing((Activity a) -> a.getPeriod() != null ? a.getPeriod().getStartDate() : LocalDate.MIN)
            .reversed()
            .thenComparing(Activity::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    Comparator<Project> projectOrder = Comparator
            .comparing((Project p) -> p.getPeriod() != null ? p.getPeriod().getStartDate() : LocalDate.MIN)
            .reversed()
            .thenComparing(Project::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    Comparator<Career> careerOrder = Comparator
            .comparing((Career c) -> c.getPeriod() != null ? c.getPeriod().getStartDate() : LocalDate.MIN)
            .reversed()
            .thenComparing(Career::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    List<ActivityResponse> activityDtos = resume.getActivities().stream()
            .sorted(activityOrder)
            .map(ActivityResponse::from)
            .toList();

    List<ProjectResponse> projectDtos = resume.getProjects().stream()
            .sorted(projectOrder)
            .map(ProjectResponse::from)
            .toList();

    List<CareerResponse> careerDtos = resume.getCareers().stream()
            .sorted(careerOrder)
            .map(CareerResponse::from)
            .toList();

    Set<CertificateDTO> certificateDtos = resume.getCertificates().stream()
            .map(CertificateDTO::from)
            .collect(Collectors.toSet());

    return new ResumeResponse(
            IntroductionResponse.from(resume.getIntroduction()),
            EducationResponse.from(resume.getEducation()),
            DesiredCompanyResponse.from(resume.getDesiredCompany()),
            careerDtos,
            activityDtos,
            projectDtos,
            certificateDtos
    );
  }
}