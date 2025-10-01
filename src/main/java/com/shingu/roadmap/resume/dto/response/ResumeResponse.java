package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Activity;
import com.shingu.roadmap.resume.domain.Project;
import com.shingu.roadmap.resume.domain.Resume;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record ResumeResponse(
        IntroductionResponse introduction,
        EducationResponse education,
        DesiredCompanyResponse desiredCompany,
        List<ActivityResponse> activities,
        List<ProjectResponse> projects
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

    List<ActivityResponse> activityDtos = resume.getActivities().stream()
            .sorted(activityOrder)
            .map(ActivityResponse::from)
            .toList();

    List<ProjectResponse> projectDtos = resume.getProjects().stream()
            .sorted(projectOrder)
            .map(ProjectResponse::from)
            .toList();

    return new ResumeResponse(
            IntroductionResponse.from(resume.getIntroduction()),
            EducationResponse.from(resume.getEducation()),
            DesiredCompanyResponse.from(resume.getDesiredCompany()),
            activityDtos,
            projectDtos
    );
  }
}