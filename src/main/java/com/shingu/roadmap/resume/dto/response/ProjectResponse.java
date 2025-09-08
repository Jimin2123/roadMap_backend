package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.resume.domain.Period;
import com.shingu.roadmap.resume.domain.Project;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ProjectResponse(
        String name,
        PeriodResponse period,
        String role,
        String url,
        String description,
        List<String> achievements,
        Set<String> techStack
) {
  public static ProjectResponse from(Project p) {
    return new ProjectResponse(
            p.getName(),
            PeriodResponse.from(p.getPeriod()),
            p.getRole(),
            p.getUrl(),
            p.getDescription(),
            p.getAchievements(),
            p.getTechStack().stream()
                    .map(Skill::getName)
                    .collect(Collectors.toSet())
    );
  }
}