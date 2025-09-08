package com.shingu.roadmap.resume.domain;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

  @Getter
  @Embeddable // 이 클래스를 다른 엔티티에 삽입할 수 있음을 선언
  @NoArgsConstructor
  public class Period {

    private LocalDate startDate; // 시작일

    private LocalDate endDate;   // 종료일 (진행 중이면 null)

    public Period(LocalDate startDate, LocalDate endDate) {
      this.startDate = startDate;
      this.endDate = endDate;
    }
  }