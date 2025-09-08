package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Activity;

public record ActivityResponse(
        String title,
        String organization,
        PeriodResponse period,
        String description
) {
  public static ActivityResponse from(Activity a) {
    if (a == null) return null;
    return new ActivityResponse(
            a.getTitle(),
            a.getOrganization(),
            PeriodResponse.from(a.getPeriod()),
            a.getDescription()
    );
  }
}