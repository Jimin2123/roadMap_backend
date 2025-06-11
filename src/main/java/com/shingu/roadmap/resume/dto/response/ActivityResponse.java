package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Activity;

public record ActivityResponse(
        String title,
        String organization,
        String period,
        String description
) {
  public static ActivityResponse from(Activity a) {
    return new ActivityResponse(
            a.getTitle(),
            a.getOrganization(),
            a.getPeriod(),
            a.getDescription()
    );
  }
}