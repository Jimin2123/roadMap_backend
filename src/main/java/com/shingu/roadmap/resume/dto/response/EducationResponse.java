package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Education;

public record EducationResponse(
        String school,
        String major,
        Double gpa,
        PeriodResponse period,
        String status
) {
  public static EducationResponse from(Education e) {
    if (e == null) return null;
    return new EducationResponse(
            e.getSchool(),
            e.getMajor(),
            e.getGpa(),
            PeriodResponse.from(e.getPeriod()),
            e.getStatus()
    );
  }
}