package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Education;

public record EducationResponse(
        String school,
        String major,
        PeriodResponse period,
        String status
) {
  public static EducationResponse from(Education e) {
    return new EducationResponse(
            e.getSchool(),
            e.getMajor(),
            PeriodResponse.from(e.getPeriod()),
            e.getStatus()
    );
  }
}