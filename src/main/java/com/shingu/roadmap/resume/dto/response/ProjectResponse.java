package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.common.domain.Skill;
import com.shingu.roadmap.resume.domain.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ProjectResponse(
        String name,
        PeriodResponse period,
        String role,
        String url,
        String description,
        List<String> achievements,
        List<String> techStack
) {
  public static ProjectResponse from(Project p) {
    if (p == null) return null;

    // 방어적 복사 + 정렬/중복 제거(필요 시 distinct)
    List<String> achievements = new ArrayList<>(p.getAchievements());
    List<String> techStack = p.getTechStack().stream()
            .map(Skill::getName)
            .distinct()              // 중복 방지 (원치 않으면 제거)
            .collect(Collectors.toList());

    return new ProjectResponse(
            p.getName(),
            PeriodResponse.from(p.getPeriod()),
            p.getRole(),
            p.getUrl(),
            p.getDescription(),
            achievements,
            techStack
    );
  }
}