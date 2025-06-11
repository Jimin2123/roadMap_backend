package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Education;

public record EducationResponse(
        String school,
        String major,
        String period,
        String status
) {
  public static EducationResponse from(Education e) {
    return new EducationResponse(
            e.getSchool(),
            e.getMajor(),
            e.getPeriod(),
            e.getStatus()
    );
  }
}