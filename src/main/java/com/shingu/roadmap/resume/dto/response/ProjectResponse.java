package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Project;

public record ProjectResponse(
        String name,
        String period,
        String techStack,
        String description
) {
  public static ProjectResponse from(Project p) {
    return new ProjectResponse(
            p.getName(),
            p.getPeriod(),
            p.getTechStack(),
            p.getDescription()
    );
  }
}