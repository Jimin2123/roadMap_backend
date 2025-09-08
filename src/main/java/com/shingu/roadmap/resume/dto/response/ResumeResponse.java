package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Resume;

import java.util.Set;
import java.util.stream.Collectors;

public record ResumeResponse(
        String introduction,
        EducationResponse education,
        Set<ActivityResponse> activities,
        Set<ProjectResponse> projects
) {
  public static ResumeResponse from(Resume resume) {
    if (resume == null) return null;

    return new ResumeResponse(
            resume.getIntroduction() != null ? resume.getIntroduction().getContent() : null,
            resume.getEducation() != null ? EducationResponse.from(resume.getEducation()) : null,
            resume.getActivities().stream().map(ActivityResponse::from).collect(Collectors.toSet()),
            resume.getProjects().stream().map(ProjectResponse::from).collect(Collectors.toSet())
    );
  }
}