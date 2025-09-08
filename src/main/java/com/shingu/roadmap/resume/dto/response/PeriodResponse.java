package com.shingu.roadmap.resume.dto.response;

import com.shingu.roadmap.resume.domain.Period;

import java.time.LocalDate;

public record PeriodResponse(LocalDate startDate, LocalDate endDate) {
  public static PeriodResponse from(Period period) {
    // Period가 null일 경우를 대비한 방어 코드
    if (period == null) {
      return null;
    }
    return new PeriodResponse(period.getStartDate(), period.getEndDate());
  }
}