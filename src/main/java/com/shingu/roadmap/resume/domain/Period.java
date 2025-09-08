package com.shingu.roadmap.resume.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.time.LocalDate;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Period {

  @Column(name = "start_date", nullable = false) // 반드시 시작일이 있어야 한다면
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;   // 진행 중이면 null

  public static Period of(LocalDate start, LocalDate end) {
    if (start == null) throw new IllegalArgumentException("startDate must not be null");
    if (end != null && end.isBefore(start)) {
      throw new IllegalArgumentException("endDate must be >= startDate");
    }
    return Period.builder().startDate(start).endDate(end).build();
  }

  public boolean isOngoing() { return endDate == null; }
}