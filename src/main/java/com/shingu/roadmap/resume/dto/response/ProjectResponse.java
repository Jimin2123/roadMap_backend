package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.resume.domain.Project;

import java.util.Set;
import java.util.stream.Collectors;

public record ProjectResponse(
        String name,
        String period,
        Set<String> techStack,
        String description
) {
  public static ProjectResponse from(Project p) {
    return new ProjectResponse(
            p.getName(),
            p.getPeriod(),
            p.getTechStack().stream().map(Skill::getName).collect(Collectors.toSet()),
            p.getDescription()
    );
  }
}